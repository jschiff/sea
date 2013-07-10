package com.getperka.sea.jms.impl;

/*
 * #%L
 * Simple Event Architecture - JMS Support
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.DrainEvent;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventTransport;
import com.getperka.sea.ext.EventTransportException;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.MessageEvent;
import com.getperka.sea.jms.SubscriptionMode;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.inject.EventSession;

/**
 * Performs the actual bridging of an {@link EventDispatch} and a JMS fabric. The JMS
 * {@link Session} and related objects are intended for use by a single thread, thus this class
 * implements a mini-reactor pattern. Any functionality that requires access to a {@link Session} is
 * implemented as an {@link Action} which is transferred from the caller to the actual thread.
 */
@Singleton
public class MessageBridge implements MessageListener {

  interface Action {
    SessionName getSessionName();

    void process(Session session) throws JMSException;
  }

  class Drain implements Action {
    /**
     * No session required.
     */
    @Override
    public SessionName getSessionName() {
      return null;
    }

    @Override
    public void process(Session session) throws JMSException {
      for (EventRouting routing : routeMap.values()) {
        if (routing.consumer != null) {
          routing.consumer.close();
          routing.consumer = null;
        }
      }
    }

    @Override
    public String toString() {
      return "Shutdown";
    }
  }

  /**
   * Contains all necessary information to send and receive an event type. The routing must be
   * executed as an {@link Action} in order to fully-populate its state.
   */
  class EventRouting implements Action {
    MessageConsumer consumer;
    Destination destination;
    final Class<? extends Event> eventType;
    boolean honorReplyTo;
    final SubscriptionOptions options;
    final Semaphore semaphore;

    public EventRouting(Class<? extends Event> eventType, SubscriptionOptions options) {
      this.eventType = eventType;
      this.options = options;
      this.semaphore =
          options.concurrencyLevel() <= 0 ? null : new Semaphore(options.concurrencyLevel());
    }

    @Override
    public SessionName getSessionName() {
      return SessionName.RECEIVE;
    }

    @Override
    public void process(Session session) throws JMSException {

      String destinationName = options.destinationName();
      if (destinationName == null || destinationName.isEmpty()) {
        destinationName = applicationName;
      }

      // Last-one-wins
      EventRouting existing = routeMap.put(eventType, this);
      if (existing != null) {
        existing.consumer.close();
      }

      SubscriptionMode mode = options.subscriptionMode();
      Destination dest;
      switch (options.profile()) {
        case ANNOUNCEMENT:
          dest = session.createTopic(destinationName);
          honorReplyTo = false;
          break;
        case SCATTER_GATHER:
          dest = session.createTopic(destinationName);
          honorReplyTo = true;
          break;
        case WORK:
          dest = session.createQueue(destinationName);
          honorReplyTo = true;
          break;
        default:
          throw new UnsupportedOperationException(options.profile().name());
      }

      if (mode.shouldSend()) {
        destination = dest;
      }

      if (mode.shouldReceive()) {
        String selector = String.format("JMSType = '%s'", transport.getTypeName(eventType));
        if (!options.messageSelector().isEmpty()) {
          selector += " AND " + options.messageSelector();
        }

        if (semaphore == null) {
          // If unthrottled, use the common receive session
          consumer = session.createConsumer(dest, selector, false);
          consumer.setMessageListener(MessageBridge.this);
        } else {
          // Otherwise, allocate a new session for the receiver thread
          session = sessions.get();
          consumer = session.createConsumer(dest, selector, false);
          String name = eventType.getSimpleName();
          new Thread(new ThrottledReceiver(name, consumer, semaphore),
              "ThrottledReceiver-" + name).start();
        }
      }
    }

    @Override
    public String toString() {
      return "Route: " + eventType.getName() + " " + options;
    }
  }

  class EventSend implements Action {
    final byte[] bytes;
    final Event event;
    final Message message;

    public EventSend(Event event, byte[] bytes) {
      this.bytes = bytes;
      this.event = event;
      this.message = null;
    }

    public EventSend(Event event, Message message) {
      this.bytes = null;
      this.event = event;
      this.message = message;
    }

    @Override
    public SessionName getSessionName() {
      return SessionName.SEND;
    }

    @Override
    public void process(Session session) throws JMSException {
      EventRouting routing = routeMap.get(event.getClass());
      Destination destination = routing.destination;
      if (routing.honorReplyTo) {
        EventMetadata meta = eventMetadata.get(event);
        if (meta.getReplyTo() != null) {
          destination = meta.getReplyTo();
        }
      }

      // Receive-only mode
      if (destination == null) {
        return;
      }

      Message toSend;
      if (bytes != null) {
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(bytes);
        toSend = bytesMessage;
      } else {
        toSend = message;
      }
      toSend.setJMSType(transport.getTypeName(event.getClass()));
      toSend.setJMSReplyTo(returnPath);

      producer.send(destination, toSend, Message.DEFAULT_DELIVERY_MODE, Message.DEFAULT_PRIORITY,
          routing.options.messageTtlUnit().toMillis(routing.options.messageTtl()));
    }

    @Override
    public String toString() {
      return "Send: " + event;
    }
  }

  class RetryOnce implements Action {
    private final Action delegate;

    public RetryOnce(Action delegate) {
      this.delegate = delegate;
    }

    @Override
    public SessionName getSessionName() {
      return delegate.getSessionName();
    }

    @Override
    public void process(Session session) throws JMSException {
      try {
        delegate.process(session);
      } catch (JMSException e) {
        if (!retryOnce) {
          throw e;
        }
        logger.info("Could not send message. Retrying once.", e);
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored) {}
        delegate.process(session);
      }
    }
  }

  enum SessionName {
    RECEIVE,
    SEND;
  }

  enum State {
    NASCENT,
    STARTING,
    STARTED,
    DRAINING,
    STOPPED;
  }

  /**
   * Uses a {@link Semaphore} to restrict the rate at which {@link MessageConsumer#receive()} is
   * called.
   */
  class ThrottledReceiver implements Runnable {
    private final MessageConsumer consumer;
    private final int defaultPermits;
    private final String name;
    private final Semaphore semaphore;

    public ThrottledReceiver(String name, MessageConsumer consumer, Semaphore semaphore) {
      this.consumer = consumer;
      this.name = name;
      this.semaphore = semaphore;
      defaultPermits = semaphore.availablePermits();
      dispatch.register(this);
    }

    @Override
    public void run() {
      while (State.STARTED.equals(state.get())) {
        try {
          // Try to acquire a permit, if this doesn't happen immediately, log it
          if (!semaphore.tryAcquire()) {
            logger.debug("Throttling {}", name);
            // Now just wait until a permit is available
            semaphore.acquireUninterruptibly();
            logger.debug("Resuming {}", name);
          }
          Message message = consumer.receive();
          if (message == null) {
            semaphore.release();
          } else {
            onMessage(message);
          }
        } catch (JMSException e) {
          logger.error("Unhandled exception while receiving message", e);
        } catch (RuntimeException e) {
          logger.error("Unhandled exception while receiving message", e);
        }
      }
    }

    /**
     * This is a hack for testing, where the {@link EventDispatch} may be repeatedly drained and
     * reset, which will cause loss of any pending {@link DispatchCompleteEvent} events, preventing
     * the receiver from operating.
     */
    @Receiver
    void reset(DrainEvent evt) {
      logger.debug("Resetting {}", name);
      semaphore.drainPermits();
      semaphore.release(defaultPermits);
    }
  }

  @Inject
  EventDispatch dispatch;
  @Inject
  EventCoder eventCoder;
  @Inject
  EventMetadataMap eventMetadata;
  @EventLogger
  @Inject
  Logger logger;
  /**
   * Reminder: Sessions aren't thread-safe.
   */
  @EventSession
  @Inject
  Provider<Session> sessions;
  @Inject
  EventTransport transport;

  private String applicationName;
  private MessageProducer producer;
  private final ConcurrentMap<Class<?>, EventRouting> routeMap =
      new ConcurrentHashMap<Class<?>, EventRouting>();
  private Queue returnPath;
  private boolean retryOnce = true;
  private final Map<SessionName, Session> sessionsByName = new
      EnumMap<SessionName, Session>(SessionName.class);
  private final AtomicReference<State> state = new AtomicReference<State>(State.NASCENT);
  private final ExecutorService svc = Executors.newSingleThreadExecutor();

  /**
   * Requires injection.
   */
  MessageBridge() {}

  /**
   * Stop dequeing new messages, but continue to accept returning messages or sending new ones.
   */
  public void drain() throws Exception {
    if (state.compareAndSet(State.STARTED, State.DRAINING)) {
      execute(new Drain(), true);
    }
  }

  public String getApplicationName() {
    return applicationName;
  }

  public boolean isSubscribed(Class<? extends Event> clazz) {
    return routeMap.containsKey(clazz);
  }

  public void maybeSendToJms(Event event, EventContext context) throws Exception {
    // Avoid send-loops
    if (this.equals(context.getUserObject())) {
      return;
    }

    // Nothing to do
    if (!routeMap.containsKey(event.getClass())) {
      return;
    }

    /*
     * We want to encode the Event on the sending thread since the event could reference some kind
     * of thread-local state (e.g. an EntityManager).
     */
    Session session = sessions.get();
    try {
      Message message = eventCoder.encode(session, event, context);
      if (message != null) {
        execute(new EventSend(event, message), false);
      }
    } finally {
      session.close();
    }
  }

  /**
   * This method is called from the JMS implementation, on its own thread. Because it does not
   * require access to a {@link Session}, this method does not enqueue an {@link Action}.
   */
  @Override
  public void onMessage(Message message) {
    try {
      Event event = eventCoder.decode(message);
      EventMetadata meta = eventMetadata.get(event);
      try {
        if (message.getJMSReplyTo() != null) {
          meta.setReplyTo(message.getJMSReplyTo());
        }
      } catch (JMSException e) {
        logger.error("Unable to determine reply-to information. " +
          "This event may be lost if re-fired.", e);
      }
      // Pass this as the dispatch context to detect send-loops in maybeSendToJms
      dispatch.fire(event, this);
    } catch (EventTransportException e) {
      logger.error("Unable to decode incoming JMS message", e);
    }
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public void setRetryOnce(boolean retryOnce) {
    this.retryOnce = retryOnce;
  }

  /**
   * Prepares the MessageBridge for routing messages. This method may be called more than once.
   */
  public void start() throws JMSException {
    if (!state.compareAndSet(State.NASCENT, State.STARTING)) {
      return;
    }

    // Allocate separate sessions for sending and receiving
    Session tx = sessions.get();
    Session rx = sessions.get();

    sessionsByName.put(SessionName.RECEIVE, rx);
    sessionsByName.put(SessionName.SEND, tx);

    // Make sure a return path is available
    returnPath = rx.createTemporaryQueue();
    rx.createConsumer(returnPath).setMessageListener(this);

    // Prepare to send messages. The destination is on a per-event basis, so no default is given.
    producer = tx.createProducer(null);

    state.set(State.STARTED);
  }

  public void stop() {
    state.set(State.STOPPED);
  }

  public void subscribe(Class<? extends Event> eventType, SubscriptionOptions options)
      throws Exception {
    if (transport.canTransport(eventType) || MessageEvent.class.isAssignableFrom(eventType)) {
      execute(new EventRouting(eventType, options), true);
      return;
    }
    throw new UnsupportedOperationException("The event type " + eventType.getCanonicalName()
      + " cannot be transported");

  }

  /**
   * If an event type is throttled, credit its semaphore.
   */
  @Receiver
  void dispatchComplete(DispatchCompleteEvent evt) {
    // Ignore any events not kicked off from a JMS message
    if (!this.equals(evt.getContext().getUserObject())) {
      return;
    }
    EventRouting r = routeMap.get(evt.getSource().getClass());
    if (r != null && r.semaphore != null) {
      r.semaphore.release();
    }
  }

  @Inject
  void inject() {
    dispatch.register(this);
  }

  private void execute(Action action, boolean sync) {
    /*
     * Depending on how well the JMS client library handles broker failovers, it may be worthwhile
     * to retry failed actions.
     */
    final Action toProcess = retryOnce ? new RetryOnce(action) : action;

    Future<?> future = svc.submit(new Callable<Void>() {
      @Override
      public Void call() {
        try {
          toProcess.process(sessionsByName.get(toProcess.getSessionName()));
        } catch (JMSException e) {
          logger.error("Unexpected JMS exception", e);
        }
        return null;
      }
    });

    if (!sync) {
      return;
    }

    try {
      future.get();
    } catch (InterruptedException e) {
      // OK, just let it go
    } catch (ExecutionException e) {
      logger.error("Unable to process action", e.getCause());
    }
  }
}
