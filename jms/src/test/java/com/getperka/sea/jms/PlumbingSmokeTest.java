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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Topic;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.jms.PlumbingSmokeTest.MyQueueEvent;
import com.getperka.sea.jms.PlumbingSmokeTest.MyQueueEventSendOnly;
import com.getperka.sea.jms.PlumbingSmokeTest.MyTopicEvent;
import com.getperka.sea.util.EventLatch;

@Subscriptions(
    applicationName = "test",
    value = {
        @Subscription(
            event = MyQueueEvent.class,
            options = @SubscriptionOptions(
                profile = EventProfile.WORK)),
        @Subscription(
            event = MyQueueEventSendOnly.class,
            options = @SubscriptionOptions(
                profile = EventProfile.WORK,
                subscriptionMode = SubscriptionMode.SEND)),
        @Subscription(
            event = MyTopicEvent.class) })
public class PlumbingSmokeTest extends JmsTestBase {

  static class MyQueueEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  static class MyQueueEventSendOnly implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  static class MyTopicEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testQueueEventReceive() throws JMSException, EventSubscriberException,
      InterruptedException {
    Queue temp = testSession.createTemporaryQueue();
    Queue queue = testSession.createQueue("test");
    assertEmpty(queue);

    EventLatch<MyQueueEvent> receiver = EventLatch.create(eventDispatch,
        MyQueueEvent.class, 1);

    MyQueueEvent event = new MyQueueEvent();
    ObjectMessage message = testSession.createObjectMessage(event);
    message.setJMSReplyTo(temp);
    message.setJMSType(event.getClass().getName());

    testSession.createProducer(queue).send(message);

    receiver.await();
    MyQueueEvent received = receiver.getEventQueue().poll();
    assertNotNull(received);
    assertNotSame(event, receiver.getEventQueue().poll());

    // Ensure that the event wasn't re-sent to the queue
    assertEmpty(queue);

    // Now re-fire the event and make sure it's in the queue
    eventDispatch.fire(received);
    MessageConsumer consumer = testSession.createConsumer(temp);
    assertNotNull(((ObjectMessage) consumer.receive()).getObject());
    assertNull(consumer.receiveNoWait());
    consumer.close();
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testQueueEventSend() throws EventSubscriberException, JMSException {
    Queue queue = testSession.createQueue("test");
    MessageConsumer consumer = testSession.createConsumer(queue);

    eventDispatch.fire(new MyQueueEventSendOnly());
    Message m = consumer.receive();
    assertEquals(MyQueueEventSendOnly.class, ((ObjectMessage) m).getObject().getClass());
    consumer.close();
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testTopicEventReceive() throws JMSException, EventSubscriberException,
      InterruptedException {
    Topic queue = testSession.createTopic("test");

    EventLatch<MyTopicEvent> receiver = EventLatch.create(eventDispatch,
        MyTopicEvent.class, 1);

    MyTopicEvent event = new MyTopicEvent();
    ObjectMessage message = testSession.createObjectMessage(event);
    message.setJMSType(event.getClass().getName());
    testSession.createProducer(queue).send(message);

    receiver.await();
    MyTopicEvent received = receiver.getEventQueue().poll();
    assertNotNull(received);
    assertNotSame(event, received);

    // Now re-fire the event and make sure it was re-published to the topic
    eventDispatch.fire(received);

    receiver.await();
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testTopicEventSend() throws EventSubscriberException, JMSException {
    Topic topic = testSession.createTopic("test");
    MessageConsumer consumer = testSession.createConsumer(topic);
    eventDispatch.fire(new MyTopicEvent());
    Message m = consumer.receive();
    assertEquals(MyTopicEvent.class, ((ObjectMessage) m).getObject().getClass());
  }

  private void assertEmpty(Queue queue) throws JMSException {
    MessageConsumer consumer = testSession.createConsumer(queue);
    assertNull(consumer.receiveNoWait());
    consumer.close();
  }
}
