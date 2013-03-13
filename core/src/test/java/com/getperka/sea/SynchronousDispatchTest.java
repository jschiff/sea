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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.Test;

import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

public class SynchronousDispatchTest {

  @EventDecoratorBinding(MyDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Decorate {}

  static class MyCoundownReceiver {
    CountDownLatch latch = new CountDownLatch(1);

    @Decorate
    @Receiver(synchronous = true)
    void countDown(MyEvent evt, EventDispatch dispatch) {
      if (evt.count.decrementAndGet() > 0) {
        dispatch.fire(evt);
      } else {
        latch.countDown();
      }
    }
  }

  static class MyDecorator implements EventDecorator<Decorate, MyEvent> {
    @Inject
    EventDispatch dispatch;

    @Override
    public Callable<Object> wrap(final EventDecorator.Context<Decorate, MyEvent> ctx) {
      final MyEvent event = ctx.getEvent();
      if (!event.countInDecorator) {
        return ctx.getWork();
      }
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          if (event.count.decrementAndGet() > 0) {
            if (event.fireLaterInDecorator) {
              ctx.fireLater(event);
            } else {
              dispatch.fire(event);
            }
            return null;
          } else {
            return ctx.getWork().call();
          }
        }
      };
    }
  }

  static class MyEvent implements Event {
    boolean countInDecorator;
    boolean fireLaterInDecorator;
    final AtomicInteger count = new AtomicInteger();
  }

  static class MyReceiver {
    Thread asyncThread;
    Thread syncThread;
    CountDownLatch latch = new CountDownLatch(2);

    @Receiver
    void async(MyEvent evt) {
      asyncThread = Thread.currentThread();
      latch.countDown();
    }

    @Receiver(synchronous = true)
    void sync(MyEvent evt) {
      syncThread = Thread.currentThread();
      latch.countDown();
    }
  }

  private EventDispatch dispatch = EventDispatchers.create();

  /**
   * Verify that synchronous receivers fire on the same thread as the caller.
   */
  @Test(timeout = TestConstants.testDelay)
  public void test() throws InterruptedException {
    MyReceiver r = new MyReceiver();
    dispatch.register(r);
    dispatch.fire(new MyEvent());
    r.latch.await();

    assertNotSame(r.asyncThread, Thread.currentThread());
    assertSame(r.syncThread, Thread.currentThread());
  }

  /**
   * Verify that synchronous events can correctly dispatch additional events.
   */
  @Test(timeout = TestConstants.testDelay)
  public void testChained() throws InterruptedException {
    MyCoundownReceiver r = new MyCoundownReceiver();
    dispatch.register(r);

    MyEvent evt = new MyEvent();
    evt.count.set(10);
    dispatch.fire(evt);

    r.latch.await();
  }

  /**
   * Verify that decorators can immediately fire synchronous events.
   */
  @Test(timeout = TestConstants.testDelay)
  public void testDecoratorFire() throws InterruptedException {
    MyCoundownReceiver r = new MyCoundownReceiver();
    dispatch.register(r);

    MyEvent evt = new MyEvent();
    evt.countInDecorator = true;
    evt.count.set(10);
    dispatch.fire(evt);

    r.latch.await();
  }

  /**
   * Verify that decorators can defer synchronous events.
   */
  @Test(timeout = TestConstants.testDelay)
  public void testDecoratorFireLater() throws InterruptedException {
    MyCoundownReceiver r = new MyCoundownReceiver();
    dispatch.register(r);

    MyEvent evt = new MyEvent();
    evt.countInDecorator = true;
    evt.fireLaterInDecorator = true;
    evt.count.set(10);
    dispatch.fire(evt);

    r.latch.await();
  }
}
