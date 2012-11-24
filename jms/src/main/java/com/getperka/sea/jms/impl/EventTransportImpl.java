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
import com.getperka.sea.jms.EventTransport;
import com.getperka.sea.jms.EventTransportException;
import com.getperka.sea.jms.inject.EventSession;

public class EventTransportImpl implements EventTransport {

  @Inject
  @EventSession
  private Session session;

  protected EventTransportImpl() {}

  @Override
  public boolean canTransport(Class<? extends Event> eventType) {
    return Serializable.class.isAssignableFrom(eventType);
  }

  @Override
  public Event decode(Message message) throws EventTransportException {
    try {
      if (message instanceof ObjectMessage) {
        return (Event) ((ObjectMessage) message).getObject();
      }
      throw new EventTransportException("Message payload of unsupported type "
        + message.getClass().getName());
    } catch (JMSException e) {
      throw new EventTransportException("Exception occurred during event transport", e);
    }
  }

  @Override
  public Message encode(Event event) throws EventTransportException {
    try {
      if (event instanceof Serializable) {
        return session.createObjectMessage((Serializable) event);
      }
      throw new UnsupportedOperationException("Received unsupported event type "
        + event.getClass().getName());
    } catch (JMSException e) {
      throw new EventTransportException("Exception occurred during event transport", e);
    }
  }

}
