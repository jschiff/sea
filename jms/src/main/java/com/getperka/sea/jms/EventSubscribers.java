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

import javax.jms.ConnectionFactory;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.ext.EventTransport;
import com.getperka.sea.jms.inject.EventConnectionFactory;
import com.getperka.sea.jms.inject.JmsEventModule;
import com.google.inject.Module;

/**
 * Creates a Guice module that can be added to {@link EventDispatchers#create(Module...)} to add
 * support for JMS.
 */
public class EventSubscribers {
  /**
   * Construct a module that enables an {@link EventDispatch} to JMS bridge. The module returned by
   * this method can be passed to {@link EventDispatchers#create(Module...)}.
   * 
   * @param connectionFactory a source of JMS connections, which must be pre-configured to vend
   *          connections via the no-arg {@link ConnectionFactory#createConnection()} method
   * @param eventTransport an optional custom EventTransport instance to convert Events to JMS
   *          Messages
   * @return a Module with the necessary configuration
   */
  public static Module createModule(
      final ConnectionFactory connectionFactory,
      final EventTransport eventTransport) {
    return new JmsEventModule() {
      @Override
      protected void configure() {
        super.configure();
        bind(ConnectionFactory.class)
            .annotatedWith(EventConnectionFactory.class)
            .toInstance(connectionFactory);
        if (eventTransport != null) {
          bind(EventTransport.class).toInstance(eventTransport);
        }
      }
    };
  }

  /**
   * Utility class.
   */
  private EventSubscribers() {}
}
