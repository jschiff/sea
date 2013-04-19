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
import com.getperka.sea.util.EventLatch;

public class ModeTest extends JmsTestBase {

  static class MyEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
    String data;
  }

  @Subscriptions(@Subscription(
      event = MyEvent.class))
  interface Normal {}

  @Subscriptions(@Subscription(
      event = MyEvent.class,
      options = @SubscriptionOptions(
          subscriptionMode = SubscriptionMode.RECEIVE,
          returnMode = ReturnMode.RETURN_TO_SENDER,
          routingMode = RoutingMode.LOCAL)))
  interface Receiver {}

  @Subscriptions(@Subscription(
      event = MyEvent.class,
      options = @SubscriptionOptions(
          subscriptionMode = SubscriptionMode.SEND)))
  interface Sender {}

  @Test
  // (timeout = TEST_TIMEOUT)
  public void test() throws EventSubscriberException, InterruptedException {
    dispatch(0).addGlobalDecorator(Normal.class);
    dispatch(1).addGlobalDecorator(Receiver.class);
    dispatch(2).addGlobalDecorator(Sender.class);

    EventLatch<MyEvent> normalLatch = EventLatch.create(dispatch(0), MyEvent.class, 0);
    EventLatch<MyEvent> receiveLatch = EventLatch.create(dispatch(1), MyEvent.class, 0);
    EventLatch<MyEvent> sendLatch = EventLatch.create(dispatch(2), MyEvent.class, 0);

    /*
     * First, try sending from the regularly-configured dispatch. Verify that the send-only queue
     * did not receive any
     */
    normalLatch.reset(0);
    receiveLatch.reset(1);
    sendLatch.reset(0);
    MyEvent event = new MyEvent();
    dispatch(0).fire(event);

    // Wait for the two latches that should have received something
    receiveLatch.await();
    // Same event object should be received
    assertEquals(0, normalLatch.getEventQueue().size());
    assertEquals(0, sendLatch.getEventQueue().size());
    assertNotNull(receiveLatch.getEventQueue().peek());
    assertNotSame(event, receiveLatch.getEventQueue().poll());

    /*
     * Try sending on the receive-only queue. The expectation is that only local receivers on the
     * receive-only queue will get the event.
     */
    event = new MyEvent();
    normalLatch.reset(0);
    receiveLatch.reset(1);
    sendLatch.reset(0);
    dispatch(1).fire(event);
    receiveLatch.await();
    assertEquals(0, normalLatch.getEventQueue().size());
    assertSame(event, receiveLatch.getEventQueue().poll());
    assertEquals(0, sendLatch.getEventQueue().size());

    /*
     * Send a message from the send-only queue and re-fire it from a receiver to ensure that the
     * return queue still works.
     */
    event = new MyEvent();
    normalLatch.reset(1);
    receiveLatch.reset(1);
    sendLatch.reset(0);
    dispatch(2).fire(event);

    normalLatch.await();
    assertFalse(normalLatch.getEventQueue().isEmpty());
    assertNotSame(event, normalLatch.getEventQueue().poll());
    receiveLatch.await();
    MyEvent received = receiveLatch.getEventQueue().poll();
    assertNotNull(received);
    assertNotSame(event, received);
    assertEquals(0, sendLatch.getEventQueue().size());

    // Resend event
    normalLatch.reset(0);
    receiveLatch.reset(1);
    sendLatch.reset(0);
    received.data = "Hello world!";
    dispatch(1).fire(received);

    // Verify local dispatch
    receiveLatch.await();
    assertSame(received, receiveLatch.getEventQueue().poll());

    // Sending from a receiving stack shouldn't go out over JMS
    assertEquals(0, normalLatch.getEventQueue().size());
    assertEquals(0, sendLatch.getEventQueue().size());
  }

  @Override
  protected int getDomainCount() {
    return 3;
  }

}
