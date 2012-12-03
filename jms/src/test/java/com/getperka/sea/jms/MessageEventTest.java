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

import static org.junit.Assert.assertEquals;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;

import com.getperka.sea.util.EventLatch;

/**
 * Tests event types that can transform themselves to and from JMS messages.
 */
public class MessageEventTest extends JmsTestBase {

  static class MyEvent implements MessageEvent {
    private String data;

    @Override
    public void copyFromMessage(Message message) throws JMSException {
      setData(((TextMessage) message).getText());
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }

    @Override
    public Message toMessage(Session session) throws JMSException {
      return session.createTextMessage(data);
    }
  }

  @Test
  public void test() throws EventSubscriberException, InterruptedException {
    subscriber(0).subscribe(MyEvent.class);
    subscriber(1).subscribe(MyEvent.class);

    MyEvent evt = new MyEvent();
    evt.setData("Hello world!");

    EventLatch<MyEvent> latch = EventLatch.create(dispatch(1), MyEvent.class, 1);

    dispatch(0).fire(evt);

    latch.await();
    assertEquals(evt.getData(), latch.getEventQueue().poll().getData());
  }

  @Override
  protected int getDomainCount() {
    return 2;
  }
}
