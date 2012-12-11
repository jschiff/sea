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
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.EventSubscriber;
import com.getperka.sea.jms.EventSubscriberException;
import com.getperka.sea.jms.EventSubscription;
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.ReturnMode;
import com.getperka.sea.jms.SubscriptionMode;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.inject.EventSession;

@SubscriptionOptions
public class EventSubscriberImpl implements EventSubscriber {

  private Provider<MessageAcknowledger> acknowledgers;
  private final SubscriptionOptions defaultOptions =
      getClass().getAnnotation(SubscriptionOptions.class);
  private Logger logger;
  private Session session;
  private final AtomicBoolean shutdown = new AtomicBoolean();
  private final ConcurrentMap<Class<? extends Event>, EventSubscriptionImpl> subscribed =
      new ConcurrentHashMap<Class<? extends Event>, EventSubscriptionImpl>();
  private Provider<EventSubscriptionImpl> subscriptions;
  private EventTransport transport;

  protected EventSubscriberImpl() {}

  @Override
  public void shutdown() {
    shutdown.set(true);

    for (EventSubscriptionImpl subscription : subscribed.values()) {
      subscription.cancel();
    }

    try {
      session.close();
    } catch (JMSException e) {
      logger.error("Exception while shutting down", e);
    }
  }

  @Override
  public EventSubscription subscribe(Class<? extends Event> eventType)
      throws EventSubscriberException {
    SubscriptionOptions options = eventType.getAnnotation(SubscriptionOptions.class);
    if (options == null) {
      options = defaultOptions;
    }

    return subscribe(eventType, options);
  }

  @Override
  public EventSubscription subscribe(Class<? extends Event> eventType, SubscriptionOptions options)
      throws EventSubscriberException {
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
      switch (options.sendMode()) {
        case QUEUE:
          destination = session.createQueue(destinationName);
          break;
        case TOPIC:
          destination = session.createTopic(destinationName);
          break;
        default:
          throw new UnsupportedOperationException(options.sendMode().name());
      }

      // Determine how a returned event should be sent
      boolean honorReplyTo = ReturnMode.RETURN_TO_SENDER.equals(options.returnMode());

      SubscriptionMode mode = options.subscriptionMode();
      EventSubscriptionImpl subscription = subscriptions.get();

      MessageProducer producer = mode.shouldSend() ? session.createProducer(destination) : null;
      subscription.subscribe(eventType, producer, honorReplyTo);

      if (mode.shouldReceive()) {
        MessageConsumer consumer;
        String selector = options.messageSelector().isEmpty() ? null : options.messageSelector();
        switch (options.sendMode()) {
          case QUEUE:
            consumer = session.createConsumer(destination, selector, options.preventEchoEffect());
            break;
          case TOPIC:
            if (options.durableSubscriberId().isEmpty()) {
              consumer = session.createConsumer(destination, selector, options.preventEchoEffect());
            } else {
              consumer = session.createDurableSubscriber((Topic) destination,
                  options.durableSubscriberId(), selector, options.preventEchoEffect());
            }
            break;
          default:
            throw new UnsupportedOperationException(options.sendMode().name());
        }

        if (mode.shouldAcknowledge()) {
          consumer.setMessageListener(acknowledgers.get().withDelegate(subscription));
        } else {
          consumer.setMessageListener(subscription);
        }
      }
      subscribed.put(eventType, subscription);
      return subscription;
    } catch (JMSException e) {
      throw new EventSubscriberException("Could not subscribe to event type", e);
    }
  }

  @Inject
  void inject(Provider<MessageAcknowledger> acknowledgers, @EventLogger Logger logger,
      @EventSession Session session, Provider<EventSubscriptionImpl> subscriptions,
      EventTransport transport) {
    this.acknowledgers = acknowledgers;
    this.logger = logger;
    this.session = session;
    this.subscriptions = subscriptions;
    this.transport = transport;
  }
}
