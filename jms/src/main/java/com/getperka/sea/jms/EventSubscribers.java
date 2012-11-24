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
import com.getperka.sea.impl.HasInjector;
import com.getperka.sea.jms.inject.EventConnectionFactory;
import com.getperka.sea.jms.inject.JmsEventModule;
import com.google.inject.Injector;

/**
 * A factory for {@link EventSubscriber} instances.
 */
public class EventSubscribers {
  /**
   * Construct an {@link EventDispatch} to JMS bridge. This method will return a new instance each
   * time it is called, allowing multiple JMS bindings and subscription sets to be established.
   * 
   * @param dispatch the EventDispatch to bind to
   * @param connectionFactory a source of JMS connections, which must be pre-configured to vend
   *          connections via the no-arg {@link ConnectionFactory#createConnection()} method
   * @return a new {@link EventSubscriber}
   */
  public static EventSubscriber create(EventDispatch dispatch, ConnectionFactory connectionFactory) {
    return create(dispatch, connectionFactory, null);
  }

  /**
   * Construct an {@link EventDispatch} to JMS bridge. This method will return a new instance each
   * time it is called, allowing multiple JMS bindings and subscription sets to be established.
   * 
   * @param dispatch the EventDispatch to bind to
   * @param connectionFactory a source of JMS connections, which must be pre-configured to vend
   *          connections via the no-arg {@link ConnectionFactory#createConnection()} method
   * @param eventTransport a custom EventTransport instance to convert Events to JMS Messages
   * @return a new {@link EventSubscriber}
   */
  public static EventSubscriber create(EventDispatch dispatch,
      final ConnectionFactory connectionFactory,
      final EventTransport eventTransport) {
    Injector injector = ((HasInjector) dispatch).getInjector();
    Injector childInjector = injector.createChildInjector(new JmsEventModule() {
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
    });
    return childInjector.getInstance(EventSubscriber.class);
  }

  /**
   * Utility class.
   */
  private EventSubscribers() {}
}
