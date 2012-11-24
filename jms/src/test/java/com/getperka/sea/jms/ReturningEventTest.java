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
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.jms.SubscriptionOptions.DestinationType;

/**
 * Verify that a ReturningEvent dispatched via a topic is routed correctly when re-fired.
 * <p>
 * The scenario simulated by this test is a scatter-gather event. An event stack fires a broadcast
 * event which contains some sort of computation to be performed by an unknown number of processors,
 * which then return copies of the event to the original sender.
 * 
 * <pre>
 *             --> stack2 --
 *             |           |
 * stack1 -----|           --> stack1
 *             |           |
 *             --> stack3 --
 * </pre>
 */
public class ReturningEventTest extends JmsTestBase {

  @SubscriptionOptions(returnMode = DestinationType.QUEUE)
  static class MyReturningEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
    private transient boolean isLocal = true;
    private String message;

    public String getMessage() {
      return message;
    }

    public boolean isLocal() {
      return isLocal;
    }

    public void setLocal(boolean local) {
      isLocal = local;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

  private static class EventCollector {
    private final java.util.Queue<MyReturningEvent> collected = new ConcurrentLinkedQueue<MyReturningEvent>();
    private final AtomicInteger numberCollected = new AtomicInteger();
    private CountDownLatch latch = new CountDownLatch(0);

    void await() throws InterruptedException {
      latch.await();
    }

    @Receiver
    void event(MyReturningEvent evt) {
      collected.add(evt);
      numberCollected.incrementAndGet();
      latch.countDown();
    }

    void reset(int expected) {
      collected.clear();
      numberCollected.set(0);
      latch = new CountDownLatch(expected);
    }
  }

  private EventDispatch eventDispatch2;
  private EventDispatch eventDispatch3;
  private EventSubscriber eventSubscriber2;
  private EventSubscriber eventSubscriber3;

  @Override
  @After
  public void after() throws JMSException {
    eventSubscriber2.shutdown();
    eventDispatch2.shutdown();
    eventSubscriber3.shutdown();
    eventDispatch3.shutdown();
    super.after();
  }

  @Override
  @Before
  public void before() throws JMSException {
    super.before();

    eventDispatch2 = EventDispatchers.create();
    eventSubscriber2 = EventSubscribers.create(eventDispatch2, connectionFactory);
    eventDispatch3 = EventDispatchers.create();
    eventSubscriber3 = EventSubscribers.create(eventDispatch3, connectionFactory);
  }

  @Test(timeout = TEST_TIMEOUT)
  public void test() throws EventSubscriberException, InterruptedException {
    EventCollector collector = new EventCollector();
    EventCollector collector2 = new EventCollector();
    EventCollector collector3 = new EventCollector();

    // Wire first two event subscribers
    eventDispatch.register(collector);
    eventDispatch2.register(collector2);
    eventDispatch3.register(collector3);

    eventSubscriber.subscribe(MyReturningEvent.class);
    eventSubscriber2.subscribe(MyReturningEvent.class);
    eventSubscriber3.subscribe(MyReturningEvent.class);

    // Fire the event from stack 1
    MyReturningEvent evt = new MyReturningEvent();
    evt.setMessage("1");
    collector2.reset(1);
    collector3.reset(1);
    eventDispatch.fire(evt);

    // Verify the event was collected in stack 2
    collector2.await();
    MyReturningEvent evt2 = collector2.collected.poll();
    assertEquals("1", evt2.getMessage());
    assertFalse(evt2.isLocal());
    // assertNotNull(evt2.getReturnQueueName());

    // Verify the event was collected in stack 3
    collector3.await();
    MyReturningEvent evt3 = collector3.collected.poll();
    assertEquals("1", evt3.getMessage());
    assertFalse(evt3.isLocal());
    // assertNotNull(evt3.getReturnQueueName());

    // Update the event in stack 2 and 3, then re-fire
    collector.reset(2);
    collector2.reset(1);
    collector3.reset(1);
    evt2.setMessage("2");
    evt2.setLocal(true);
    eventDispatch2.fire(evt2);
    evt3.setMessage("3");
    evt3.setLocal(true);
    eventDispatch3.fire(evt3);

    // Verify the event was collected in stack 1
    collector.await();
    assertEquals(2, collector.numberCollected.get());
    while (!collector.collected.isEmpty()) {
      MyReturningEvent returnedEvent = collector.collected.poll();
      assertTrue("2".equals(returnedEvent.getMessage()) || "3".equals(returnedEvent.getMessage()));
    }

    // Verify the event counts in the other stacks was from the local re-firing
    collector2.await();
    assertEquals(1, collector2.numberCollected.get());
    assertTrue(collector2.collected.poll().isLocal());
    collector3.await();
    assertEquals(1, collector3.numberCollected.get());
    assertTrue(collector3.collected.poll().isLocal());
  }
}
