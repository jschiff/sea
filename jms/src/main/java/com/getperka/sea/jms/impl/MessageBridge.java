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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
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
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.EventTransportException;
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
public class MessageBridge extends Thread implements MessageListener {

  interface Action {
    void process(Session session) throws JMSException, EventTransportException;
  }

  class Drain implements Action {
    @Override
    public void process(Session session) throws JMSException, EventTransportException {
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

    public EventRouting(Class<? extends Event> eventType, SubscriptionOptions options) {
      this.eventType = eventType;
      this.options = options;
    }

    @Override
    public void process(Session session) throws JMSException, EventTransportException {

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

        consumer = session.createConsumer(dest, selector, false);
        consumer.setMessageListener(MessageBridge.this);
      }
    }

    @Override
    public String toString() {
      return "Route: " + eventType.getName() + " " + options;
    }
  }

  class EventSend implements Action {
    final Event event;
    final Message message;

    public EventSend(Event event, Message message) {
      this.event = event;
      this.message = message;
    }

    @Override
    public void process(Session session) throws JMSException, EventTransportException {
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

      message.setJMSType(transport.getTypeName(event.getClass()));
      message.setJMSReplyTo(returnPath);

      producer.send(destination, message);
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
    public void process(Session session) throws JMSException, EventTransportException {
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

  /**
   * An null sentinal Exception used by {@link #execute(Action)}.
   */
  private static final Exception NO_EXCEPTION = new Exception();

  @Inject
  EventDispatch dispatch;
  @Inject
  EventMetadataMap eventMetadata;
  @EventLogger
  @Inject
  Logger logger;
  /**
   * Sessions aren't thread-safe, use {@link #threadLocalSession} instead.
   */
  @EventSession
  @Inject
  Provider<Session> sessions;
  @Inject
  EventTransport transport;

  private String applicationName;
  private final SynchronousQueue<Exception> done = new SynchronousQueue<Exception>();
  private MessageProducer producer;
  private final ConcurrentMap<Class<?>, EventRouting> routeMap = new ConcurrentHashMap<Class<?>, EventRouting>();
  private final AtomicBoolean drain = new AtomicBoolean();
  private final ThreadLocal<Session> threadLocalSession = new ThreadLocal<Session>() {
    @Override
    protected Session initialValue() {
      return sessions.get();
    }
  };
  private final SynchronousQueue<Action> todo = new SynchronousQueue<Action>();
  private Queue returnPath;
  private boolean retryOnce = true;

  MessageBridge() {
    setDaemon(true);
    setName("SEA MessageThread");
  }

  /**
   * Stop dequeing new messages, but continue to accept returning messages or sending new ones.
   */
  public void drain() throws Exception {
    if (drain.compareAndSet(false, true)) {
      execute(new Drain());
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
     * Pass a thread-local session to the EventTransport, since we don't want to give it access to
     * the main session used by the run-loop. The only reason the Session is being passed is to
     * provide a factory for Message objects. Keeping the session open provides a noticeable
     * performance boost, and there shouldn't be too many active sending threads. If this proves to
     * be a problem, it would be possible to pass in a Session-facade that only allows Message
     * creation, using an Action to actually produce the result.
     */
    Message message = transport.encode(threadLocalSession.get(), event, context);
    if (message == null) {
      return;
    }

    execute(new EventSend(event, message));
  }

  /**
   * This method is called from the JMS implementation, on its own thread. Because it does not
   * require access to a {@link Session}, this method does not enqueue an {@link Action}.
   */
  @Override
  public void onMessage(Message message) {
    try {
      Event event = transport.decode(message);
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
      logger.error("Unable to dispatch incoming JMS message", e);
    }
  }

  @Override
  public void run() {
    Session session = threadLocalSession.get();
    try {
      producer = session.createProducer(null);
      returnPath = session.createTemporaryQueue();
      session.createConsumer(returnPath).setMessageListener(this);
    } catch (JMSException e) {
      logger.error("Could not obtain JMS resources", e);
      return;
    }

    try {
      while (true) {
        try {
          Action action = todo.take();
          logger.trace("Processing {}", action);
          action.process(session);
          done.put(NO_EXCEPTION);
        } catch (Exception e) {
          try {
            done.put(e);
          } catch (InterruptedException ignored) {}
        }
      }
    } finally {
      logger.error(getClass().getSimpleName() + " is exiting");
    }
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public void setRetryOnce(boolean retryOnce) {
    this.retryOnce = retryOnce;
  }

  public void subscribe(Class<? extends Event> eventType, SubscriptionOptions options)
      throws Exception {
    if (!transport.canTransport(eventType)) {
      throw new UnsupportedOperationException("The event type " + eventType.getCanonicalName()
        + " cannot be transported");
    }

    execute(new EventRouting(eventType, options));
  }

  private void execute(Action action) throws Exception {
    /*
     * Depending on how well the JMS client library handles broker failovers, it may be worthwhile
     * to retry failed actions.
     */
    if (retryOnce) {
      action = new RetryOnce(action);
    }

    todo.put(action);
    Exception ex = done.take();
    if (!NO_EXCEPTION.equals(ex)) {
      throw ex;
    }
  }
}
