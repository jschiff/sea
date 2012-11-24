package com.getperka.sea.jms;

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

import javax.jms.Message;

import com.getperka.sea.Event;
import com.getperka.sea.jms.impl.EventTransportImpl;
import com.google.inject.ImplementedBy;

/**
 * Defines how {@link Event} instances should be transformed to and from {@link Message} instances.
 * The default implementation of EventTransport support types that implements {@link Serializable}.
 */
@ImplementedBy(EventTransportImpl.class)
public interface EventTransport {
  /**
   * Return {@code true} if the given event can can be transported by the implementation.
   */
  boolean canTransport(Class<? extends Event> eventType);

  /**
   * Construct an {@link Event} instance from a JMS {@link Message}.
   */
  Event decode(Message message) throws EventTransportException;

  /**
   * Construct a JMS {@link Message} from an {@link Event} instance.
   */
  Message encode(Event event) throws EventTransportException;
}
