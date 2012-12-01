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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.Registration;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.EventSubscription;
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.EventTransportException;
import com.getperka.sea.jms.inject.EventSession;

/**
 * Manages a single subscription to a JMS destination.
 */
public class EventSubscriptionImpl implements EventSubscription, MessageListener {

  private MessageProducer defaultMessageProducer;
  private EventDispatch dispatch;
  private Registration dispatchRegistration;
  private EventMetadataMap eventMetadata;
  private Class<? extends Event> eventType;
  private boolean honorReplyTo;
  private Logger logger;
  private MessageConsumer messageConsumer;
  private Destination returnQueue;
  private Session session;
  private final AtomicBoolean shutdown = new AtomicBoolean();
  private EventTransport transport;

  protected EventSubscriptionImpl() {}

  @Override
  public void cancel() {
    if (shutdown.getAndSet(true)) {
      return;
    }
    dispatchRegistration.cancel();
    try {
      messageConsumer.close();
    } catch (JMSException e) {
      logger.warn("Could not close MessageConsumer", e);
    }
  }

  @Override
  public void onMessage(Message message) {
    try {
      Event event = transport.decode(eventType, message);
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

  public void subscribe(Class<? extends Event> eventType, MessageProducer defaultProducer,
      boolean honorReplyTo) throws JMSException {
    this.defaultMessageProducer = defaultProducer;
    this.eventType = eventType;
    this.honorReplyTo = honorReplyTo;

    returnQueue = session.createTemporaryQueue();
    messageConsumer = session.createConsumer(returnQueue);
    messageConsumer.setMessageListener(this);
    dispatchRegistration = dispatch.register(this);
  }

  @Inject
  void inject(EventDispatch dispatch, EventMetadataMap eventMetadataMap,
      @EventLogger Logger logger, @EventSession Session session,
      EventTransport transport) {
    this.dispatch = dispatch;
    this.eventMetadata = eventMetadataMap;
    this.logger = logger;
    this.session = session;
    this.transport = transport;
  }

  @Receiver
  void maybeSendToJms(Event event) {
    try {
      MessageProducer producer = getTargetProducer(event);
      if (producer != null) {
        sendEvent(producer, event);

        /*
         * If the MessageProducer is a temporary one, close it to avoid the need to wait for the
         * finalizers to run.
         */
        if (producer != defaultMessageProducer) {
          producer.close();
        }
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
    // Prevent event instances dispatched from the subscriber from being immediately re-broadcast
    EventMetadata meta = eventMetadata.get(event);
    if (meta.isSquelched()) {
      return null;
    }

    // Events being re-fired may need to be routed to specific locations
    if (honorReplyTo) {
      Destination dest = meta.getReplyTo();
      if (dest != null) {
        return session.createProducer(dest);
      }
    }

    // Use the event type's default queue, if it is registered anywhere.
    // TODO: Allow subscribing to entire type hierarchies?
    return defaultMessageProducer;
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
