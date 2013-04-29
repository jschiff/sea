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

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.TopicConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

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

  protected static ConnectionFactory connectionFactory;
  private static EmbeddedJMS jmsServer;

  @AfterClass
  public static void afterClass() throws Exception {
    jmsServer.stop();
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    connectionFactory = startServer();
  }

  /**
   * Cribbed from the hornetq jms/embedded example.
   */
  private static ConnectionFactory startServer() throws Exception {
    // Step 1. Create HornetQ core configuration, and set the properties accordingly
    Configuration configuration = new ConfigurationImpl();
    configuration.setPersistenceEnabled(false);
    configuration.setJournalDirectory("target/data/journal");
    configuration.setSecurityEnabled(false);
    configuration.getAcceptorConfigurations()
        .add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));

    TransportConfiguration connectorConfig = new TransportConfiguration(
        InVMConnectorFactory.class.getName());

    configuration.getConnectorConfigurations().put("connector", connectorConfig);

    // Step 2. Create the JMS configuration
    JMSConfiguration jmsConfig = new JMSConfigurationImpl();

    // Step 3. Configure the JMS ConnectionFactory
    ArrayList<String> connectorNames = new ArrayList<String>();
    connectorNames.add("connector");
    ConnectionFactoryConfiguration cfConfig = new ConnectionFactoryConfigurationImpl("cf", false,
        connectorNames, "/cf");
    jmsConfig.getConnectionFactoryConfigurations().add(cfConfig);

    // Step 4. Configure the JMS Queue
    JMSQueueConfiguration queueConfig = new JMSQueueConfigurationImpl("test", null, false);
    jmsConfig.getQueueConfigurations().add(queueConfig);

    TopicConfiguration topicConfig = new TopicConfigurationImpl("test");
    jmsConfig.getTopicConfigurations().add(topicConfig);

    // Step 5. Start the JMS Server using the HornetQ core server and the JMS configuration
    jmsServer = new EmbeddedJMS();
    jmsServer.setConfiguration(configuration);
    jmsServer.setJmsConfiguration(jmsConfig);
    jmsServer.start();
    System.out.println("Started Embedded JMS Server");

    // Step 6. Lookup JMS resources defined in the configuration
    ConnectionFactory cf = (ConnectionFactory) jmsServer.lookup("/cf");
    return cf;
  };

  /**
   * The 0-th element of {@link #eventDispatches}.
   */
  protected EventDispatch eventDispatch;

  private List<EventDispatch> eventDispatches = new ArrayList<EventDispatch>();

  protected Session testSession;

  private Connection testConnection;

  @After
  public void after() throws JMSException {
    for (EventDispatch dispatch : eventDispatches) {
      ((HasInjector) dispatch).getInjector().getInstance(SubscriptionObserver.class).drain();
      dispatch.shutdown();
    }
    testSession.close();
    testConnection.close();
  }

  @Before
  public void before() throws Exception {

    testConnection = connectionFactory.createConnection();
    testConnection.start();
    testSession = testConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

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
