package com.getperka.sea.jms.decorator;

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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.jms.EventSubscriberException;
import com.getperka.sea.jms.JmsTestBase;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.decorator.SuppressLocalEventsTest.MyEchoedEvent;
import com.getperka.sea.jms.decorator.SuppressLocalEventsTest.MyEvent;
import com.getperka.sea.util.EventLatch;

@SuppressLocalEvents({ MyEvent.class, MyEchoedEvent.class })
public class SuppressLocalEventsTest extends JmsTestBase {

  @SubscriptionOptions(preventEchoEffect = false)
  static class MyEchoedEvent implements Event, Serializable {}

  static class MyEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  @Test(timeout = TEST_TIMEOUT)
  public void test() throws EventSubscriberException {
    dispatch(0).addGlobalDecorator(getClass());
    dispatch(1).addGlobalDecorator(getClass());
    subscriber(0).subscribe(MyEvent.class);
    subscriber(1).subscribe(MyEvent.class);

    // Don't actually expect the local latch to catch anything
    EventLatch<MyEvent> localLatch = EventLatch.create(dispatch(0), MyEvent.class, 1);
    EventLatch<DispatchCompleteEvent> localDispatchLatch = EventLatch.create(dispatch(0),
        DispatchCompleteEvent.class, 1);

    // Expect a single event to be received on the "remote" side.
    EventLatch<MyEvent> remoteLatch = EventLatch.create(dispatch(1), MyEvent.class, 1);

    MyEvent localEvent = new MyEvent();
    dispatch(0).fire(localEvent);
    localDispatchLatch.awaitUninterruptibly();
    DispatchCompleteEvent complete = localDispatchLatch.getEventQueue().poll();
    assertSame(localEvent, complete.getSource());

    // The local latch should not have received anything
    assertTrue(localLatch.getEventQueue().isEmpty());

    // Now check that the remote side received something
    remoteLatch.awaitUninterruptibly();
    MyEvent remoteEvent = remoteLatch.getEventQueue().poll();
    assertNotSame(localEvent, remoteEvent);
    // Re-fire remote event
    dispatch(1).fire(remoteEvent);

    localLatch.awaitUninterruptibly();
    MyEvent localEchoed = localLatch.getEventQueue().poll();
    assertNotSame(localEvent, localEchoed);
    assertNotSame(remoteEvent, localEchoed);
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testEchoEffect() throws EventSubscriberException {
    dispatch(0).addGlobalDecorator(getClass());
    subscriber(0).subscribe(MyEchoedEvent.class);

    EventLatch<MyEchoedEvent> latch = EventLatch.create(dispatch(0), MyEchoedEvent.class, 1);
    EventLatch<DispatchCompleteEvent> complete = EventLatch.create(dispatch(0),
        DispatchCompleteEvent.class, 2);

    MyEchoedEvent evt = new MyEchoedEvent();
    dispatch(0).fire(evt);

    latch.awaitUninterruptibly();
    assertNotSame(evt, latch.getEventQueue().poll());

    complete.awaitUninterruptibly();
  }

  @Override
  protected int getDomainCount() {
    return 2;
  }
}
