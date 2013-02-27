package com.getperka.sea.decoration;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.TestConstants;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.impl.HasInjector;
import com.getperka.sea.util.EventLatch;

public class ExclusiveTest {

  class SleepyReceiver {
    private final String name;

    SleepyReceiver(String name) {
      this.name = name;
    }

    /**
     * Add the receiver's name to the queue both before and after the sleep occurs.
     */
    @Exclusive
    @Receiver
    void receive(MyEvent evt) {
      ReentrantLock lock = (ReentrantLock) filter.getLockForTesting(evt);
      assertTrue(lock.isHeldByCurrentThread());

      queue.add(name);
      try {
        Thread.sleep(10);
      } catch (InterruptedException ignored) {
        // Don't care
      } finally {
        if (queue.size() < 4) {
          assertTrue(lock.hasQueuedThreads());
        } else {
          assertFalse(lock.hasQueuedThreads());
        }
        queue.add(name);
      }
    }
  }

  private static class MyEvent implements Event {}

  private EventDispatch dispatch = EventDispatchers.create();
  private ExclusiveFilter filter;
  private final Queue<String> queue = new ConcurrentLinkedQueue<String>();

  @Test(timeout = TestConstants.testDelay)
  public void test() {
    filter = ((HasInjector) dispatch).getInjector().getInstance(ExclusiveFilter.class);

    SleepyReceiver a = new SleepyReceiver("a");
    SleepyReceiver b = new SleepyReceiver("b");
    SleepyReceiver c = new SleepyReceiver("c");
    EventLatch<DispatchCompleteEvent> latch = EventLatch.create(dispatch,
        DispatchCompleteEvent.class, 1);

    dispatch.register(a);
    dispatch.register(b);
    dispatch.register(c);

    MyEvent event = new MyEvent();
    dispatch.fire(event);
    latch.awaitUninterruptibly();

    assertEquals(6, queue.size());

    // Assert that the receiver names occur in adjacent pairs
    for (Iterator<String> it = queue.iterator(); it.hasNext();) {
      assertEquals(it.next(), it.next());
    }

    ReentrantLock lock = (ReentrantLock) filter.getLockForTesting(event);
    assertFalse(lock.isLocked());
  }
}
