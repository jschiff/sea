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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;

/**
 * Provides setup of basic services for JMS-related tests.
 */
public class JmsTestBase {
  protected static final int TEST_TIMEOUT = 5000;

  protected ConnectionFactory connectionFactory;
  protected EventDispatch eventDispatch;
  protected EventSubscriber eventSubscriber;
  protected Session testSession;

  @After
  public void after() throws JMSException {
    eventSubscriber.shutdown();
    eventDispatch.shutdown();
    testSession.close();
  }

  @Before
  public void before() throws JMSException {
    eventDispatch = EventDispatchers.create();

    // Use a non-persistent, loopback test server
    connectionFactory = new ActiveMQConnectionFactory(
        "vm://localhost?broker.persistent=false");

    Connection connection = connectionFactory.createConnection();
    connection.start();
    testSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    eventSubscriber = EventSubscribers.create(eventDispatch, connectionFactory);
  }
}
