package com.getperka.sea;

/*
 * #%L
 * Simple Event Architecture - Core
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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
import static com.getperka.sea.TestConstants.testDelay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.SuspendedEvent;
import com.getperka.sea.util.EventLatch;

public class SuspendedEventTest {

  class MyEvent implements Event {
    final AtomicBoolean hasSuspended = new AtomicBoolean();
  }

  class MyReceiver {
    final SynchronousQueue<MyEvent> aQueue = new SynchronousQueue<MyEvent>();
    final SynchronousQueue<MyEvent> bQueue = new SynchronousQueue<MyEvent>();
    final SynchronousQueue<SuspendedEvent> suspended = new SynchronousQueue<SuspendedEvent>();

    @Receiver
    void a(MyEvent evt) throws InterruptedException {
      aQueue.put(evt);
    }

    @Receiver
    void b(MyEvent evt, EventContext ctx) throws InterruptedException {
      bQueue.put(evt);
      if (evt.hasSuspended.compareAndSet(false, true)) {
        suspended.put(ctx.suspend());
      }
    }
  }

  @Test(timeout = testDelay)
  public void test() throws InterruptedException {
    EventDispatch dispatch = EventDispatchers.create();
    MyReceiver r = new MyReceiver();
    dispatch.register(r);

    EventLatch<DispatchCompleteEvent> latch =
        EventLatch.create(dispatch, DispatchCompleteEvent.class, 1);

    MyEvent evt = new MyEvent();
    dispatch.fire(evt);

    assertSame(evt, r.aQueue.take());
    assertSame(evt, r.bQueue.take());
    SuspendedEvent suspended = r.suspended.take();
    assertTrue(latch.getEventQueue().isEmpty());

    suspended.resume();
    assertSame(evt, r.bQueue.take());
    latch.await();
    assertEquals(1, latch.getEventQueue().size());
    DispatchCompleteEvent dispatchComplete = latch.getEventQueue().poll();
    // Two MyReceiver methods and one for the EventLatch
    assertEquals(3, dispatchComplete.getResults().size());

    // Verify that only the b receiver was called
    assertTrue(r.aQueue.isEmpty());

    try {
      suspended.resume();
      fail();
    } catch (IllegalStateException expected) {}
  }
}
