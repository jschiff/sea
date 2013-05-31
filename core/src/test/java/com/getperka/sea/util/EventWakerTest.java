package com.getperka.sea.util;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.getperka.sea.ext.SuspendedEvent;

/**
 * Unit test for {@link EventWaker}.
 */
public class EventWakerTest {
  static class FakeSuspendedEvent implements SuspendedEvent {
    final AtomicBoolean didResume = new AtomicBoolean();
    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void resume() {
      didResume.set(true);
      latch.countDown();
      // throw new IllegalStateException("Expected");
    }
  }

  @Test
  public void testRecent() throws InterruptedException {
    EventWaker<Object> waker = new EventWaker<Object>();
    Object key = new Object();

    // Initial state
    assertFalse(waker.isRecent(key, false));

    // No delay set
    waker.signal(key);
    assertFalse(waker.isRecent(key, false));

    // Signal with a window set
    waker.setRecentWindow(1, TimeUnit.MINUTES);
    waker.signal(key);

    // Verify removal behavior
    assertTrue(waker.isRecent(key, false));
    assertTrue(waker.isRecent(key, true));
    assertFalse(waker.isRecent(key, true));

    // Test clear
    waker.signal(key);
    waker.clear();
    assertFalse(waker.isRecent(key, false));

    // Change the window and verify cleanup
    waker.signal(key);
    Thread.sleep(1);
    waker.setRecentWindow(1, TimeUnit.NANOSECONDS);
    assertFalse(waker.isRecent(key, false));

    waker.signal(key);
    waker.setRecentWindow(-11, TimeUnit.NANOSECONDS);
    assertFalse(waker.isRecent(key, false));
  }

  @Test
  public void testResumeAll() {
    EventWaker<Object> waker = new EventWaker<Object>();
    Object key = new Object();
    FakeSuspendedEvent event = new FakeSuspendedEvent();

    waker.resumeAfterSignal(key, event, 1, TimeUnit.DAYS);
    waker.signalAll();
    assertTrue(event.didResume.get());
  }

  @Test
  public void testResumeSignal() {
    EventWaker<Object> waker = new EventWaker<Object>();
    Object key = new Object();
    FakeSuspendedEvent event = new FakeSuspendedEvent();

    waker.resumeAfterSignal(key, event, 1, TimeUnit.DAYS);
    assertEquals(1, waker.getPendingEventCount(key));
    assertFalse(event.didResume.get());
    waker.signal(key);
    assertEquals(0, waker.getPendingEventCount(key));
    assertTrue(event.didResume.get());
  }

  @Test
  public void testResumeTimeout() throws InterruptedException {
    EventWaker<Object> waker = new EventWaker<Object>();
    Object key = new Object();
    FakeSuspendedEvent event = new FakeSuspendedEvent();

    waker.resumeAfterSignal(key, event, 1, TimeUnit.NANOSECONDS);
    Thread.sleep(1);
    assertTrue(event.didResume.get());
  }
}
