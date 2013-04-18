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

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.impl.HasInjector;
import com.getperka.sea.jms.impl.SubscriptionObserver;
import com.google.inject.Module;

/**
 * Provides setup of basic services for JMS-related tests.
 * <p>
 * The test subclass will be used as a global decorator for any {@link EventDispatch} instances
 * created.
 */
public class JmsTestBase {
  protected static final int TEST_TIMEOUT = 1000;

  protected ConnectionFactory connectionFactory;
  /**
   * The 0-th element of {@link #eventDispatches}.
   */
  protected EventDispatch eventDispatch;
  private List<EventDispatch> eventDispatches = new ArrayList<EventDispatch>();;
  protected Session testSession;

  @After
  public void after() throws JMSException {
    for (EventDispatch dispatch : eventDispatches) {
      ((HasInjector) dispatch).getInjector().getInstance(SubscriptionObserver.class).stop();
      dispatch.shutdown();
    }
    testSession.close();
  }

  @Before
  public void before() throws JMSException {
    // Use a non-persistent, loopback test server
    connectionFactory = new ActiveMQConnectionFactory(
        "vm://localhost?broker.persistent=false");

    Connection connection = connectionFactory.createConnection();
    connection.start();
    testSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    for (int i = 0, j = getDomainCount(); i < j; i++) {
      Module module = EventSubscribers.createModule(connectionFactory, null);
      EventDispatch d = EventDispatchers.create(module);
      d.addGlobalDecorator(getClass());
      ((HasInjector) d).getInjector().getInstance(SubscriptionObserver.class).start();

      eventDispatches.add(d);
      if (i == 0) {
        eventDispatch = d;
      }
    }
  }

  protected EventDispatch dispatch(int index) {
    return eventDispatches.get(index);
  }

  protected int getDomainCount() {
    return 1;
  }
}
