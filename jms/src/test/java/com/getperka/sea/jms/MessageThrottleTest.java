package com.getperka.sea.jms;
/*
 * #%L
 * Simple Event Architecture - JMS Support
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.Receiver;

/**
 * Test JMS message throttling by attempting to send a large number of events of a single type, and
 * ensure that the maximum number of concurrent receiver invocations doesn't exceed the limit.
 */
public class MessageThrottleTest extends JmsTestBase {
  static class MyReceiver {
    final CyclicBarrier barrier;
    final CountDownLatch latch;
    final AtomicBoolean ok = new AtomicBoolean(true);
    final Semaphore sem;

    public MyReceiver(int concurrencyLevel, int totalEvents) {
      barrier = new CyclicBarrier(concurrencyLevel);
      latch = new CountDownLatch(totalEvents);
      sem = new Semaphore(concurrencyLevel);
    }

    @Receiver
    void receive(ThrottleEvent evt) {
      /*
       * Verifies that no more than a given number of threads are executing at once, or acquisition
       * of the semaphore will fail.
       */
      if (!sem.tryAcquire()) {
        ok.set(false);
        fail();
      }
      // Ensure that we are seeing concurrent behavior by waiting for other threads to get here
      try {
        barrier.await();
      } catch (InterruptedException e) {
        ok.set(false);
      } catch (BrokenBarrierException e) {
        ok.set(false);
      }
      latch.countDown();
      sem.release();
    }
  }

  static class ThrottleEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  @Subscriptions(
      applicationName = "test",
      value = {
          @Subscription(
              event = ThrottleEvent.class,
              options = @SubscriptionOptions(
                  concurrencyLevel = 1)) })
  @Test(timeout = 5 * TEST_TIMEOUT)
  public void testOne() throws Exception {
    test("testOne");
  }

  @Subscriptions(
      applicationName = "test",
      value = {
          @Subscription(
              event = ThrottleEvent.class,
              options = @SubscriptionOptions(
                  concurrencyLevel = 10)) })
  @Test(timeout = 5 * TEST_TIMEOUT)
  public void testTen() throws Exception {
    test("testTen");
  }

  @Subscriptions(
      applicationName = "test",
      value = {
          @Subscription(
              event = ThrottleEvent.class,
              options = @SubscriptionOptions(
                  concurrencyLevel = 2)) })
  @Test(timeout = 5 * TEST_TIMEOUT)
  public void testTwo() throws Exception {
    test("testTwo");
  }

  @Override
  protected int getDomainCount() {
    return 2;
  }

  private void test(String methodName) throws Exception {
    Method m = getClass().getDeclaredMethod(methodName);

    dispatch(0).addGlobalDecorator(m);
    dispatch(1).addGlobalDecorator(m);

    Subscriptions subscriptions = m.getAnnotation(Subscriptions.class);

    int concurrencyLevel = subscriptions.value()[0].options().concurrencyLevel();
    int totalEvents = concurrencyLevel * 100;
    MyReceiver r = new MyReceiver(concurrencyLevel, totalEvents);
    dispatch(1).register(r);

    for (int i = 0; i < totalEvents; i++) {
      dispatch(0).fire(new ThrottleEvent());
    }
    r.latch.await();
    assertTrue(r.ok.get());
  }
}
