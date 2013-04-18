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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.jms.EventSubscriberException;
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.ReturnMode;
import com.getperka.sea.jms.RoutingMode;
import com.getperka.sea.jms.SubscriptionMode;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.inject.EventSession;

/**
 * The EventSubscriber controls how events are routed across a JMS domain. Specific event types are
 * registered with the EventSubscriber by the {@link SubscriptionObserver}.
 */
@Singleton
public class EventSubscriber {

  @Inject
  @EventSession
  Session session;
  @Inject
  Provider<EventSubscription> subscriptions;
  @Inject
  EventTransport transport;

  private final AtomicBoolean shutdown = new AtomicBoolean();
  private final ConcurrentMap<Class<? extends Event>, EventSubscription> subscribed =
      new ConcurrentHashMap<Class<? extends Event>, EventSubscription>();

  protected EventSubscriber() {}

  public boolean isSubscribed(Class<? extends Event> eventType) {
    return subscribed.containsKey(eventType);
  }

  /**
   * Terminate all subscriptions.
   */
  public void shutdown() {
    if (shutdown.getAndSet(true)) {
      return;
    }

    for (EventSubscription subscription : subscribed.values()) {
      subscription.cancel();
    }
    subscribed.clear();
  }

  public EventSubscription subscribe(Class<? extends Event> eventType,
      SubscriptionOptions options) throws EventSubscriberException {
    if (shutdown.get()) {
      throw new IllegalStateException("EventSubscriber has been shut down");
    }
    if (!transport.canTransport(eventType)) {
      throw new UnsupportedOperationException("The event type " + eventType.getCanonicalName()
        + " cannot be transported");
    }
    try {
      // Allow the destination name to be overridden
      String destinationName = options.destinationName().isEmpty() ?
          eventType.getCanonicalName() : options.destinationName();

      // Determine where an otherwise-undirected event should be sent
      Destination destination;
      switch (options.destinationType()) {
        case QUEUE:
          destination = session.createQueue(destinationName);
          break;
        case TOPIC:
          destination = session.createTopic(destinationName);
          break;
        default:
          throw new UnsupportedOperationException(options.destinationType().name());
      }

      // Determine how a returned event should be sent
      boolean honorReplyTo = ReturnMode.RETURN_TO_SENDER.equals(options.returnMode());

      SubscriptionMode mode = options.subscriptionMode();
      EventSubscription subscription = subscriptions.get();

      MessageProducer producer = mode.shouldSend() ? session.createProducer(destination) : null;
      subscription.subscribe(eventType, producer, honorReplyTo);

      if (mode.shouldReceive()) {
        MessageConsumer consumer;
        // Prevents the local subscriber stack from seeing its own JMS messages
        boolean noLocal = RoutingMode.LOCAL.equals(options.routingMode());
        String selector = options.messageSelector().isEmpty() ? null : options.messageSelector();
        switch (options.destinationType()) {
          case QUEUE:
            consumer = session.createConsumer(destination, selector, noLocal);
            break;
          case TOPIC:
            if (options.durableSubscriberId().isEmpty()) {
              consumer = session.createConsumer(destination, selector, noLocal);
            } else {
              consumer = session.createDurableSubscriber((Topic) destination,
                  options.durableSubscriberId(), selector, noLocal);
            }
            break;
          default:
            throw new UnsupportedOperationException(options.destinationType().name());
        }

        consumer.setMessageListener(subscription);
      }

      // Implement last-one-wins policy
      EventSubscription existing = subscribed.put(eventType, subscription);
      if (existing != null) {
        existing.cancel();
      }
      return subscription;
    } catch (JMSException e) {
      throw new EventSubscriberException("Could not subscribe to event type", e);
    }
  }

  void fire(Event event, EventContext context) {
    for (EventSubscription subscription : subscribed.values()) {
      subscription.maybeSendToJms(event, context);
    }
  }
}
