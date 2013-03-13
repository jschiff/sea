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

import java.io.Serializable;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.EventTransportException;
import com.getperka.sea.jms.MessageEvent;
import com.getperka.sea.jms.inject.EventSession;
import com.google.inject.Injector;

public class EventTransportImpl implements EventTransport {

  private Injector injector;
  private Session session;

  protected EventTransportImpl() {}

  @Override
  public boolean canTransport(Class<? extends Event> eventType) {
    return MessageEvent.class.isAssignableFrom(eventType) ||
      Serializable.class.isAssignableFrom(eventType);
  }

  @Override
  public <T extends Event> T decode(Class<T> eventType, Message message)
      throws EventTransportException {
    try {
      // Support for SEA's custom MessageEvent
      if (MessageEvent.class.isAssignableFrom(eventType)) {
        T toReturn = injector.getInstance(eventType);
        ((MessageEvent) toReturn).copyFromMessage(message);
        return toReturn;
      }

      // Assume that any incoming ObjectMessage contains an Event
      if (Serializable.class.isAssignableFrom(eventType)) {
        if (!(message instanceof ObjectMessage)) {
          throw new EventTransportException("Expected a JMS ObjectEvent, received a "
            + message.getClass().getName());
        }
        Serializable event = ((ObjectMessage) message).getObject();
        try {
          return eventType.cast(event);
        } catch (ClassCastException e) {
          throw new EventTransportException("Incoming JMS ObjectMessage contained a "
            + event.getClass().getName() + " which is not assignable to the expected event type "
            + eventType.getName());
        }
      }

      throw new EventTransportException("Untransportable event type " + eventType.getName());
    } catch (JMSException e) {
      throw new EventTransportException("Exception occurred during event transport", e);
    }
  }

  @Override
  public Message encode(Event event, EventContext context) throws EventTransportException {
    try {
      if (event instanceof MessageEvent) {
        return ((MessageEvent) event).toMessage(session);
      }
      if (event instanceof Serializable) {
        return session.createObjectMessage((Serializable) event);
      }
      throw new UnsupportedOperationException("Received unsupported event type "
        + event.getClass().getName());
    } catch (JMSException e) {
      throw new EventTransportException("Exception occurred during event transport", e);
    }
  }

  @Inject
  void inject(Injector injector, @EventSession Session session) {
    this.injector = injector;
    this.session = session;
  }

}
