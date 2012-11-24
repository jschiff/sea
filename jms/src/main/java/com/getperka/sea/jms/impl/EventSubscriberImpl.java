package com.getperka.sea.jms.impl;

/*
 * #%L
 * Simple Event Architecture - JMS Support
 * %%
 * Copyright (C) 2012 Perka Inc.
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.EventSubscriber;
import com.getperka.sea.jms.EventSubscriberException;
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.EventTransportException;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.inject.EventSession;

@SubscriptionOptions
public class EventSubscriberImpl implements EventSubscriber, MessageListener {

  static class EventReference {
    private final WeakReference<Event> evt;
    private final int hashCode;

    public EventReference(Event evt, ReferenceQueue<? super Event> queue) {
      this.evt = new WeakReference<Event>(evt, queue);
      this.hashCode = System.identityHashCode(evt);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof EventReference)) {
        return false;
      }
      EventReference other = (EventReference) o;
      Event myEvent = evt.get();
      return myEvent != null && myEvent == other.evt.get();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  private final SubscriptionOptions defaultOptions =
      getClass().getAnnotation(SubscriptionOptions.class);
  private EventDispatch dispatch;
  private EventMetadataMap eventMetadata;
  private Logger logger;
  private Queue returnQueue;
  private Session session;
  private final AtomicBoolean shutdown = new AtomicBoolean();
  private final ConcurrentMap<Class<? extends Event>, MessageProducer> subscribed =
      new ConcurrentHashMap<Class<? extends Event>, MessageProducer>();
  private EventTransport transport;

  protected EventSubscriberImpl() {}

  @Override
  public void onMessage(Message message) {
    if (shutdown.get()) {
      return;
    }
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
      meta.setSquelched(true);
      dispatch.fire(event);
    } catch (EventTransportException e) {
      logger.error("Unable to dispatch incoming JMS message", e);
    }
  }

  @Override
  public void shutdown() {
    try {
      shutdown.set(true);
      session.close();
    } catch (JMSException e) {
      logger.error("Exception while shutting down", e);
    }
  }

  @Override
  public void subscribe(Class<? extends Event> eventType) throws EventSubscriberException {
    SubscriptionOptions options = eventType.getAnnotation(SubscriptionOptions.class);
    if (options == null) {
      options = defaultOptions;
    }

    subscribe(eventType, options);
  }

  @Override
  public void subscribe(Class<? extends Event> eventType, SubscriptionOptions options)
      throws EventSubscriberException {
    if (shutdown.get()) {
      return;
    }
    if (!transport.canTransport(eventType)) {
      throw new UnsupportedOperationException("The event type " + eventType.getCanonicalName()
        + " cannot be transported");
    }
    try {
      // Allow the destination name to be overridden
      String destinationName = options.destinationName().isEmpty() ?
          eventType.getCanonicalName() : options.destinationName();

      Destination destination;
      MessageConsumer consumer;
      switch (options.sendMode()) {
        case QUEUE:
          destination = session.createQueue(destinationName);
          consumer = session.createConsumer(destination, null, true);
          break;
        case TOPIC:
          destination = session.createTopic(destinationName);
          if (options.durableSubscriberId().isEmpty()) {
            consumer = session.createConsumer(destination, null, true);
          } else {
            consumer = session.createDurableSubscriber((Topic) destination,
                options.durableSubscriberId(), null, true);
          }
          break;
        default:
          throw new UnsupportedOperationException(options.sendMode().name());
      }

      consumer.setMessageListener(this);
      MessageProducer producer = session.createProducer(destination);
      subscribed.put(eventType, producer);
    } catch (JMSException e) {
      throw new EventSubscriberException("Could not subscribe to event type", e);
    }
  }

  @Inject
  void inject(EventDispatch dispatch, EventMetadataMap eventMetadata, @EventLogger Logger logger,
      @EventSession Session session, EventTransport transport) throws JMSException {
    this.dispatch = dispatch;
    this.eventMetadata = eventMetadata;
    this.logger = logger;
    this.session = session;
    this.transport = transport;

    // Create a temporary return queue and receive its messages
    returnQueue = session.createTemporaryQueue();
    session.createConsumer(returnQueue).setMessageListener(this);

    dispatch.register(this);
  }

  @Receiver
  void maybeSendToJms(Event event) {
    try {
      MessageProducer producer = getTargetProducer(event);
      if (producer != null) {
        sendEvent(producer, event);
      }
    } catch (JMSException e) {
      logger.error("Unable to send event to JMS service", e);
    }
  }

  /**
   * Returns the {@link MessageProducer} that the event should be sent to or {@code null} if the
   * Event cannot or should not be sent.
   */
  private MessageProducer getTargetProducer(Event event) throws JMSException {
    if (shutdown.get()) {
      return null;
    }

    EventMetadata meta = eventMetadata.get(event);
    // Prevent event instances dispatched from the subscriber from being immediately re-broadcast
    if (meta.isSquelched()) {
      return null;
    }

    // Events being re-fired may need to be routed to specific locations
    Destination dest = meta.getReplyTo();
    if (dest != null) {
      return session.createProducer(dest);
    }

    // Use the event type's default queue, if it is registered anywhere.
    // TODO: Allow subscribing to entire type hierarchies?
    return subscribed.get(event.getClass());
  }

  private void sendEvent(MessageProducer producer, Event event) {
    Throwable ex;
    try {
      Message message = transport.encode(event);
      message.setJMSReplyTo(returnQueue);
      producer.send(message);
      return;
    } catch (JMSException e) {
      ex = e;
    } catch (EventTransportException e) {
      ex = e;
    }
    logger.error("Unable to send Event via JMS message", ex);
  }
}
