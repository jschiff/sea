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

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;

import com.getperka.sea.inject.EventLogger;

public class MessageAcknowledger implements MessageListener {
  private MessageListener delegate;

  @EventLogger
  @Inject
  private Logger logger;

  protected MessageAcknowledger() {}

  @Override
  public void onMessage(Message message) {
    try {
      message.acknowledge();
    } catch (JMSException e) {
      logger.error("Unable to acknowledge JMS message", e);
    }
    delegate.onMessage(message);
  }

  MessageAcknowledger withDelegate(MessageListener delegate) {
    this.delegate = delegate;
    return this;
  }
}
