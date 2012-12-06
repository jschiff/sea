package com.getperka.sea.inject;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.inject.DecoratorScopeTest.A;
import com.getperka.sea.inject.DecoratorScopeTest.B;
import com.getperka.sea.util.EventLatch;
import com.google.inject.ProvisionException;

/**
 * Verify that {@link DecoratorScoped} objects have lifecyles constrained to an individual decorator
 * invocation.
 */
@A
@B
public class DecoratorScopeTest {

  @EventDecoratorBinding(DecoratorA.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface A {}

  @EventDecoratorBinding(DecoratorB.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface B {}

  static class DecoratorA implements EventDecorator<A, MyEvent> {
    @Inject
    Provider<MyDecoratorScoped> provider;
    @Inject
    MyDecoratorScoped scoped;
    @Inject
    MyDecoratorScoped scoped2;
    @Inject
    Unscoped unscoped;
    @Inject
    Unscoped unscoped2;

    @Override
    public Callable<Object> wrap(final Context<A, MyEvent> ctx) {
      assertSame(scoped, provider.get());
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          assertNotNull(scoped);
          assertSame(scoped, scoped2);
          assertNotSame(unscoped, unscoped2);

          // The provider scope has exited by the time the actual work is invoked
          try {
            provider.get();
            fail();
          } catch (ProvisionException expected) {}

          ctx.getEvent().a = scoped;
          return ctx.getWork().call();
        }
      };
    }
  }

  static class DecoratorB implements EventDecorator<B, MyEvent> {
    @Inject
    Provider<MyDecoratorScoped> provider;
    @Inject
    MyDecoratorScoped scoped;
    @Inject
    MyDecoratorScoped scoped2;
    @Inject
    Unscoped unscoped;
    @Inject
    Unscoped unscoped2;

    @Override
    public Callable<Object> wrap(final Context<B, MyEvent> ctx) {
      assertSame(scoped, provider.get());
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          assertNotNull(scoped);
          assertSame(scoped, scoped2);
          assertNotSame(unscoped, unscoped2);

          // The provider scope has exited by the time the actual work is invoked
          try {
            provider.get();
            fail();
          } catch (ProvisionException expected) {}

          ctx.getEvent().b = scoped;
          return ctx.getWork().call();
        }
      };
    }
  }

  @DecoratorScoped
  static class MyDecoratorScoped {}

  class MyEvent implements Event {
    MyDecoratorScoped a;
    MyDecoratorScoped b;
  }

  static class Unscoped {}

  @Test
  // (timeout = TestConstants.testDelay)
  public void test() {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.addGlobalDecorator(getClass());

    EventLatch<MyEvent> latch = EventLatch.create(dispatch, MyEvent.class, 1);
    dispatch.fire(new MyEvent());

    latch.awaitUninterruptibly();
    MyEvent evt = latch.getEventQueue().poll();
    assertNotNull(evt);
    assertNotNull(evt.a);
    assertNotNull(evt.b);
    assertNotSame(evt.a, evt.b);
  }
}
