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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventTransport;
import com.getperka.sea.ext.EventTransportException;
import com.getperka.sea.jms.MessageEvent;
import com.google.inject.Injector;

/**
 * Converts {@link Event Events} to and from {@link Message Messages}.
 */
public class EventCoder {
  @Inject
  Injector injector;
  @Inject
  EventTransport transport;

  private static final Charset UTF8 = Charset.forName("UTF-8");

  /**
   * Requires injection.
   */
  protected EventCoder() {}

  /**
   * Recreate an {@link Event} from a {@link Message}.
   */
  public Event decode(Message message) throws EventTransportException {
    try {
      String messageEventType = message.getStringProperty("MessageEvent");
      if (messageEventType != null) {
        Class<?> eventClass = Class.forName(messageEventType, false,
            Thread.currentThread().getContextClassLoader());
        MessageEvent event = injector.getInstance(eventClass.asSubclass(MessageEvent.class));
        event.copyFromMessage(message);
        return event;
      }
    } catch (ClassNotFoundException e) {
      throw new EventTransportException("Could not create event instance", e);
    } catch (JMSException e) {
      throw new EventTransportException("Could not read JMS property", e);
    }

    InputStream in;
    if (message instanceof BytesMessage) {
      final BytesMessage bytes = (BytesMessage) message;
      // Incremental reads from the BytesMessage
      in = new InputStream() {
        @Override
        public int read() throws IOException {
          try {
            return bytes.readUnsignedByte();
          } catch (MessageEOFException e) {
            return -1;
          } catch (JMSException e) {
            throw new IOException(e);
          }
        }

        @Override
        public int read(byte[] b) throws IOException {
          try {
            return bytes.readBytes(b);
          } catch (JMSException e) {
            throw new IOException(e);
          }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          if (len == 0) {
            return 0;
          }
          byte[] buffer = new byte[8192];
          int read;
          try {
            read = bytes.readBytes(buffer, Math.min(len, 8192));
          } catch (JMSException e) {
            throw new IOException("Could not read from JMS message", e);
          }
          if (read == -1) {
            return -1;
          }
          System.arraycopy(buffer, 0, b, off, read);
          return read;
        }
      };
    } else if (message instanceof TextMessage) {
      // Convert text messages (e.g. from a management console) to a UTF8 byte stream
      TextMessage text = (TextMessage) message;
      try {
        in = new ByteArrayInputStream(text.getText().getBytes(UTF8));
      } catch (JMSException e) {
        throw new EventTransportException("Could not read message text", e);
      }
    } else {
      throw new EventTransportException("Unhandled Message type " + message.getClass().getName());
    }

    return transport.decode(in);
  }

  /**
   * Convert the {@link Event} to a {@link Message}.
   */
  public Message encode(Session session, Event event, EventContext context)
      throws EventTransportException {
    try {
      if (event instanceof MessageEvent) {
        try {
          Message message = ((MessageEvent) event).toMessage(session);
          message.setStringProperty("MessageEvent", event.getClass().getName());
          return message;
        } finally {
          session.close();
        }
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      if (!transport.encode(event, context, output)) {
        return null;
      }
      BytesMessage message = session.createBytesMessage();
      message.writeBytes(output.toByteArray());
      return message;
    } catch (JMSException e) {
      throw new EventTransportException("Unable to encode event", e);
    }
  }
}
