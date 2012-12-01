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
import static org.junit.Assert.assertSame;

import java.io.Serializable;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.testing.EventCountDownLatch;

public class ModeTest extends JmsTestBase {

  static class MyEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
    String data;
  }

  @SubscriptionOptions(subscriptionMode = SubscriptionMode.RECEIVE,
      returnMode = ReturnMode.RETURN_TO_SENDER)
  interface Receiver {}

  @SubscriptionOptions(subscriptionMode = SubscriptionMode.SEND)
  interface Sender {}

  @Test(timeout = TEST_TIMEOUT)
  public void test() throws EventSubscriberException, InterruptedException {
    subscriber(0).subscribe(MyEvent.class);
    subscriber(1)
        .subscribe(MyEvent.class, Receiver.class.getAnnotation(SubscriptionOptions.class));
    subscriber(2).subscribe(MyEvent.class, Sender.class.getAnnotation(SubscriptionOptions.class));

    EventCountDownLatch<MyEvent> bothLatch = EventCountDownLatch.create(
        dispatch(0), MyEvent.class, 0);
    EventCountDownLatch<MyEvent> receiveLatch = EventCountDownLatch.create(
        dispatch(1), MyEvent.class, 0);
    EventCountDownLatch<MyEvent> sendLatch = EventCountDownLatch.create(
        dispatch(2), MyEvent.class, 0);

    /*
     * First, try sending from the regularly-configured dispatch. Verify that the send-only queue
     * did not receive any
     */
    bothLatch.reset(1);
    receiveLatch.reset(1);
    sendLatch.reset(0);
    MyEvent event = new MyEvent();
    dispatch(0).fire(event);

    // Wait for the two latches that should have received something
    bothLatch.await();
    receiveLatch.await();
    // Same event object should be received
    assertSame(event, bothLatch.getEventQueue().poll());
    assertEquals(0, sendLatch.getEventQueue().size());
    assertNotNull(receiveLatch.getEventQueue().peek());
    assertNotSame(event, receiveLatch.getEventQueue().poll());

    /*
     * Try sending on the receive-only queue. The expectation is that only local receivers on the
     * receive-only queue will get the event.
     */
    event = new MyEvent();
    bothLatch.reset(0);
    receiveLatch.reset(1);
    sendLatch.reset(0);
    dispatch(1).fire(event);
    receiveLatch.await();
    assertEquals(0, bothLatch.getEventQueue().size());
    assertSame(event, receiveLatch.getEventQueue().poll());
    assertEquals(0, sendLatch.getEventQueue().size());

    /*
     * Send a message from the send-only queue and re-fire it from a receiver to ensure that the
     * return queue still works.
     */
    event = new MyEvent();
    bothLatch.reset(1);
    receiveLatch.reset(1);
    sendLatch.reset(1);
    dispatch(2).fire(event);

    bothLatch.await();
    assertFalse(bothLatch.getEventQueue().isEmpty());
    assertNotSame(event, bothLatch.getEventQueue().poll());
    receiveLatch.await();
    MyEvent received = receiveLatch.getEventQueue().poll();
    assertNotNull(received);
    assertNotSame(event, received);
    sendLatch.await();
    assertSame(event, sendLatch.getEventQueue().poll());

    // Resend event
    bothLatch.reset(0);
    receiveLatch.reset(1);
    sendLatch.reset(1);
    received.data = "Hello world!";
    dispatch(1).fire(received);

    // Verify local dispatch
    receiveLatch.await();
    assertSame(received, receiveLatch.getEventQueue().poll());
    // Verify return-over-JMS behavior
    sendLatch.await();
    assertEquals("Hello world!", sendLatch.getEventQueue().poll().data);
  }

  @Override
  protected int getDomainCount() {
    return 3;
  }

}
