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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class DrainTest {

  static class MyEvent implements Event {}

  static class MyReceiver {
    final CountDownLatch hasStarted = new CountDownLatch(1);
    final CountDownLatch waitFor = new CountDownLatch(1);

    @Receiver
    void myEvent(MyEvent evt) throws InterruptedException {
      hasStarted.countDown();
      waitFor.await();
    }
  }

  @Test(timeout = TestConstants.testDelay)
  public void test() throws InterruptedException {
    final EventDispatch dispatch = EventDispatchers.create();

    MyReceiver r = new MyReceiver();
    dispatch.register(r);
    dispatch.fire(new MyEvent());

    // Wait for the receiver method to start up
    r.hasStarted.await();

    // Start a task that wants to drain the thread
    final CountDownLatch waitingOnDrain = new CountDownLatch(1);
    Thread t = new Thread() {
      @Override
      public void run() {
        waitingOnDrain.countDown();
        dispatch.setDraining(true);
      }
    };
    t.start();

    // Wait for thread to spin up
    waitingOnDrain.await();
    assertTrue(t.isAlive());

    // Allow the receiver to finish
    r.waitFor.countDown();
    // Then wait for the draining thread to exit
    t.join();
  }
}
