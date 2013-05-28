package com.getperka.sea.impl;

/*
 * #%L
 * Simple Event Architecture - Core
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventTransport;
import com.getperka.sea.ext.EventTransportException;

/**
 * Writes {@link Serializable} {@link Event Events}.
 */
public class SerializableEventTransport implements EventTransport {

  @Override
  public boolean canTransport(Class<? extends Event> eventType) {
    return Serializable.class.isAssignableFrom(eventType);
  }

  @Override
  public Event decode(InputStream input) throws EventTransportException {
    try {
      ObjectInput data = new ObjectInputStream(input);
      try {
        return (Event) data.readObject();
      } finally {
        data.close();
      }
    } catch (ClassNotFoundException e) {
      throw new EventTransportException("Could not find requested class", e);
    } catch (IOException e) {
      throw new EventTransportException("Unable to decode event", e);
    }
  }

  @Override
  public boolean encode(Event event, EventContext context, OutputStream output)
      throws EventTransportException {
    try {
      ObjectOutputStream data = new ObjectOutputStream(output);
      data.writeObject(event);
      data.close();
      return true;
    } catch (IOException e) {
      throw new EventTransportException("Unable to encode event", e);
    }
  }

  @Override
  public String getTypeName(Class<? extends Event> eventType) {
    return eventType.getName();
  }

}
