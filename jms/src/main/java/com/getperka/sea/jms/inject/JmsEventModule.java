package com.getperka.sea.jms.inject;

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

import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class JmsEventModule extends AbstractModule {

  @Override
  protected void configure() {}

  @EventConnection
  @Provides
  @Singleton
  Connection connection(
      @EventConnectionFactory ConnectionFactory factory) throws JMSException {
    Connection conn = factory.createConnection();
    return conn;
  }

  @EventSession
  @Provides
  Session session(@EventConnection Connection conn) throws JMSException {
    return conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
  }
}
