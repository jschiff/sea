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
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;

import org.junit.Test;

import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.util.EventLatch;

/**
 * Ensure that global decorators will have effects on previously-registered receivers.
 */
public class GlobalDecoratorTest {

  @EventDecoratorBinding(Decorator1.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Decorated1 {}

  @EventDecoratorBinding(Decorator2.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Decorated2 {}

  @Decorated1
  static class Decorator1 implements EventDecorator<Decorated1, MyEvent> {
    @Override
    public Callable<Object> wrap(final EventDecorator.Context<Decorated1, MyEvent> ctx) {
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          ctx.getEvent().d1 = true;
          return ctx.getWork().call();
        }
      };
    }
  }

  @Decorated2
  static class Decorator2 implements EventDecorator<Decorated2, MyEvent> {
    @Override
    public Callable<Object> wrap(final EventDecorator.Context<Decorated2, MyEvent> ctx) {
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          ctx.getEvent().d2 = true;
          return ctx.getWork().call();
        }
      };
    }
  }

  static class MyEvent implements Event {
    boolean d1;
    boolean d2;
  }

  @Test(timeout = TestConstants.testDelay)
  public void test() {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.addGlobalDecorator(Decorator1.class);

    // Register this latch after the first decorator is installed
    EventLatch<MyEvent> latch = EventLatch.create(dispatch, MyEvent.class, 1);

    // Make sure decoration works
    MyEvent e1 = new MyEvent();
    dispatch.fire(e1);
    latch.awaitUninterruptibly();
    assertTrue(e1.d1);
    assertFalse(e1.d2);

    // Re-attach latch
    latch.reset(1);
    // Add the second decorator
    dispatch.addGlobalDecorator(Decorator2.class);

    // Fire another event and make sure both decorators are working
    MyEvent e2 = new MyEvent();
    dispatch.fire(e2);
    latch.awaitUninterruptibly();
    assertTrue(e2.d1);
    assertTrue(e2.d2);

  }

}
