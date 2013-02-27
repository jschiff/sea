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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.util.EventLatch;

public class WeakReceiverTest {
  static class MyEvent implements Event {
    boolean actedUpon;
  }

  @Test
  public void test() {
    EventDispatch dispatch = EventDispatchers.create();

    final AtomicReference<ReceiverTarget> receiverTarget = new AtomicReference<ReceiverTarget>();
    // Create a latch
    EventLatch<MyEvent> latch = new EventLatch<MyEvent>(dispatch, 1) {
      @Receiver
      void event(MyEvent evt, ReceiverTarget target) {
        receiverTarget.set(target);
        evt.actedUpon = true;
        countDown(evt);
      }
    };

    // Test that the latch produces observable results
    assertTrue(latch.awaitSingleEventAfter(new MyEvent(), 1, TimeUnit.SECONDS).actedUpon);
    assertNotNull(receiverTarget.get());

    // Reset the latch
    latch.reset(1);
    // Get a weak reference to the latch
    Reference<Object> latchRef = new WeakReference<Object>(latch);
    // Now remove what should be the only hard reference to the latch
    latch = null;
    // Force garbage-collection
    System.gc();
    // If the GC worked, the reference will return null. If it didn't, skip the test
    assumeTrue(latchRef.get() == null);

    // Create and fire an event, waiting for its dispatch
    MyEvent evt = new MyEvent();
    EventLatch.create(dispatch, DispatchCompleteEvent.class, 1)
        .awaitSingleEventAfter(evt, 1, TimeUnit.SECONDS);

    // Shouldn't see anything happen
    assertFalse(evt.actedUpon);

    // Check that the target doesn't explode if re-dispatched with a dead reference
    DispatchResult res = receiverTarget.get().dispatch(new MyEvent(), new EventContext() {
      @Override
      public long getSequenceNumber() {
        return 0;
      }

      @Override
      public Object getUserObject() {
        return null;
      }
    });
    assertFalse(res.wasReceived());
  }
}
