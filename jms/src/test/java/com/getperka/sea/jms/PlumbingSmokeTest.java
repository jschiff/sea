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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Topic;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.Receiver;
import com.getperka.sea.jms.SubscriptionOptions.DestinationType;

public class PlumbingSmokeTest extends JmsTestBase {

  static class EventReceiver {
    CountDownLatch latch = new CountDownLatch(1);
    MyQueueEvent queueEvent;
    int queueEvents;
    MyTopicEvent topicEvent;
    int topicEvents;

    @Receiver
    void queueEvent(MyQueueEvent evt) {
      queueEvent = evt;
      queueEvents++;
      latch.countDown();
    }

    @Receiver
    void topicEvent(MyTopicEvent evt) {
      topicEvent = evt;
      topicEvents++;
      latch.countDown();
    }
  }

  @SubscriptionOptions(sendMode = DestinationType.QUEUE)
  static class MyQueueEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  static class MyTopicEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  @Test
  public void testMultipleInstances() {
    EventSubscriber subscriber2 = EventSubscribers.create(eventDispatch, connectionFactory);
    assertNotSame(eventSubscriber, subscriber2);
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testQueueEventReceive() throws JMSException, EventSubscriberException,
      InterruptedException {
    Queue queue = testSession.createQueue(MyQueueEvent.class.getCanonicalName());
    assertEmpty(queue);

    eventSubscriber.subscribe(MyQueueEvent.class);

    EventReceiver receiver = new EventReceiver();
    eventDispatch.register(receiver);

    testSession.createProducer(queue).send(testSession.createObjectMessage(new MyQueueEvent()));
    receiver.latch.await();
    assertNotNull(receiver.queueEvent);
    assertEquals(1, receiver.queueEvents);

    // Ensure that the event wasn't re-sent to the queue
    assertEmpty(queue);

    // Now re-fire the event and make sure it's in the queue
    eventDispatch.fire(receiver.queueEvent);
    assertNotNull(((ObjectMessage) testSession.createConsumer(queue).receive()).getObject());
    assertEmpty(queue);
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testQueueEventSend() throws EventSubscriberException, JMSException {
    Queue queue = testSession.createQueue(MyQueueEvent.class.getCanonicalName());

    eventSubscriber.subscribe(MyQueueEvent.class);

    eventDispatch.fire(new MyQueueEvent());
    MessageConsumer consumer = testSession.createConsumer(queue);
    Message m = consumer.receive();
    assertEquals(MyQueueEvent.class, ((ObjectMessage) m).getObject().getClass());
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testTopicEventReceive() throws JMSException, EventSubscriberException,
      InterruptedException {
    Topic queue = testSession.createTopic(MyTopicEvent.class.getCanonicalName());
    MessageConsumer consumer = testSession.createConsumer(queue);

    eventSubscriber.subscribe(MyTopicEvent.class);

    EventReceiver receiver = new EventReceiver();
    eventDispatch.register(receiver);

    testSession.createProducer(queue).send(testSession.createObjectMessage(new MyTopicEvent()));
    // Drain this view of the topic
    assertNotNull(((ObjectMessage) consumer.receive()).getObject());
    assertNull(consumer.receiveNoWait());

    receiver.latch.await();
    assertNotNull(receiver.topicEvent);
    assertEquals(1, receiver.topicEvents);

    // Ensure that the event wasn't re-sent to the queue
    assertNull(consumer.receiveNoWait());

    // Now re-fire the event and make sure it was published to the topic
    eventDispatch.fire(receiver.topicEvent);
    assertNotNull(((ObjectMessage) consumer.receive()).getObject());
    assertNull(consumer.receiveNoWait());
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testTopicEventSend() throws EventSubscriberException, JMSException {
    Topic topic = testSession.createTopic(MyTopicEvent.class.getCanonicalName());
    MessageConsumer consumer = testSession.createConsumer(topic);
    eventSubscriber.subscribe(MyTopicEvent.class);
    eventDispatch.fire(new MyTopicEvent());
    Message m = consumer.receive();
    assertEquals(MyTopicEvent.class, ((ObjectMessage) m).getObject().getClass());
  }

  private void assertEmpty(Queue queue) throws JMSException {
    assertFalse(testSession.createBrowser(queue).getEnumeration().hasMoreElements());
  }
}