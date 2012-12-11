package com.getperka.sea.util;
/*
 * #%L
 * Simple Event Architecture - Core
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

import static com.getperka.sea.TestConstants.testDelay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;

public class EventLatchTest {

  static class MyEvent implements Event {}

  private EventDispatch dispatch = EventDispatchers.create();

  @Test(timeout = testDelay)
  public void testAwait() throws InterruptedException {
    EventLatch<Event> latch = EventLatch.create(dispatch, 1);
    assertTrue(latch.isCollecting());
    MyEvent evt = new MyEvent();
    dispatch.fire(evt);
    latch.await();
    assertSame(evt, latch.getEventQueue().poll());

    // A second await should be a no-op
    assertFalse(latch.isCollecting());
    latch.await();
  }

  @Test(timeout = testDelay)
  public void testAwaitTimed() throws InterruptedException {
    EventLatch<Event> latch = EventLatch.create(dispatch, 1);
    assertTrue(latch.isCollecting());

    // Verify the timeout occurs, but does not affect the collecting state
    latch.await(1, TimeUnit.MILLISECONDS);
    assertTrue(latch.isCollecting());

    MyEvent evt = new MyEvent();
    dispatch.fire(evt);
    latch.await();
    assertSame(evt, latch.getEventQueue().poll());

    // A second await should be a no-op
    assertFalse(latch.isCollecting());
    latch.await(1, TimeUnit.HOURS);
  }

  @Test(timeout = testDelay)
  public void testAwaitUninterruptably() {
    EventLatch<Event> latch = EventLatch.create(dispatch, 1);
    assertTrue(latch.isCollecting());

    MyEvent evt = new MyEvent();
    dispatch.fire(evt);
    latch.awaitUninterruptibly();
    assertSame(evt, latch.getEventQueue().poll());

    // A second await should be a no-op
    assertFalse(latch.isCollecting());
    latch.awaitUninterruptibly();
  }

  @Test(timeout = testDelay)
  public void testMultipleCollections() {
    EventLatch<MyEvent> latch = EventLatch.create(dispatch, MyEvent.class, 10);
    for (int i = 0; i < 10; i++) {
      dispatch.fire(new MyEvent());
    }
    latch.awaitUninterruptibly();
    assertEquals(10, latch.getEventQueue().size());
  }

  @Test(timeout = testDelay)
  public void testReset() {
    EventLatch<MyEvent> latch = EventLatch.create(dispatch, MyEvent.class, 0);

    assertFalse(latch.isCollecting());
    dispatch.fire(new MyEvent());

    latch.reset(1);
    assertTrue(latch.isCollecting());

    MyEvent evt = new MyEvent();
    dispatch.fire(evt);
    latch.awaitUninterruptibly();
    assertSame(evt, latch.getEventQueue().poll());

    // Ensure re-attaching works
    latch.reset(1);
    evt = new MyEvent();
    dispatch.fire(evt);
    latch.awaitUninterruptibly();
    assertSame(evt, latch.getEventQueue().poll());

    // Check attach and immediate detach
    latch.reset(1);
    latch.reset(2);
    latch.reset(0);
    assertFalse(latch.isCollecting());
  }
}
