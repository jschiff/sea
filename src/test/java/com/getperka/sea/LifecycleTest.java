package com.getperka.sea;

/*
 * #%L
 * Simple Event Architecture
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the correct number of instances of a receiving class are constructed.
 */
public class LifecycleTest {

  static class MyReceiver {
    /**
     * Static methods should not trigger instantiation.
     */
    @Receiver
    public static void staticReceiver(Event event) {
      latch.countDown();
    }

    public MyReceiver() {
      instanceCount.incrementAndGet();
    }

    @Receiver
    public void receive(Event event) {
      latch.countDown();
    }
  }

  @Singleton
  static class MySingletonReceiver {
    /**
     * Static methods should not trigger instantiation.
     */
    @Receiver
    public static void staticReceiver(Event event) {
      latch.countDown();
    }

    public MySingletonReceiver() {
      instanceCount.incrementAndGet();
    }

    @Receiver
    public void receive(Event event) {
      latch.countDown();
    }
  }

  static AtomicInteger instanceCount;
  static CountDownLatch latch;

  @Before
  public void before() {
    instanceCount = new AtomicInteger();
    latch = new CountDownLatch(20);
  }

  /**
   * Tests implicit instance creation.
   */
  @Test(timeout = 1000)
  public void test() throws InterruptedException {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(MyReceiver.class);

    for (int i = 0; i < 10; i++) {
      dispatch.fire(new Event() {});
    }

    latch.await();

    assertEquals(10, instanceCount.get());
  }

  /**
   * Tests giving an explicit instance to {@link EventDispatch}.
   */
  @Test(timeout = 1000)
  public void testExplicitInstance() throws InterruptedException {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(new MyReceiver());

    for (int i = 0; i < 10; i++) {
      dispatch.fire(new Event() {});
    }

    latch.await();

    assertEquals(1, instanceCount.get());
  }

  /**
   * Tests the interation of the {@link Singleton} annotation.
   */
  @Test(timeout = 1000000)
  public void testSingletonInstance() throws InterruptedException {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(MySingletonReceiver.class);

    for (int i = 0; i < 10; i++) {
      dispatch.fire(new Event() {});
    }

    latch.await();

    assertEquals(1, instanceCount.get());
  }

}
