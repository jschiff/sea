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

import static com.getperka.sea.TestConstants.testDelay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.getperka.sea.BaseCompositeEvent;
import com.getperka.sea.BaseCompositeEvent.DefaultFacets;
import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

public class CompositeEventTest {
  @EventDecoratorBinding(BarDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Bar {}

  public static class BarDecorator implements EventDecorator<Bar, BarEvent> {
    @Override
    public Callable<Object> wrap(Context<Bar, BarEvent> ctx) {
      ctx.getEvent().bar = true;
      assertNotSame(ctx.getEvent(), ctx.getOriginalEvent());
      assertEquals(MyCompoundEvent.class, ctx.getOriginalEvent().getClass());
      return ctx.getWork();
    }
  }

  public static class BarEvent implements Event {
    boolean bar;
  }

  @EventDecoratorBinding(FooDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Foo {}

  public static class FooDecorator implements EventDecorator<Foo, FooEvent> {
    @Override
    public Callable<Object> wrap(Context<Foo, FooEvent> ctx) {
      ctx.getEvent().foo = true;
      return ctx.getWork();
    }
  }

  public static class FooEvent implements Event {
    boolean foo;
  }

  @DefaultFacets({ FooEvent.class, BarEvent.class })
  public static class MyCompoundEvent extends BaseCompositeEvent {}

  public static class MyReceiver {
    CountDownLatch latch = new CountDownLatch(1);
    MyCompoundEvent evt;

    @Bar
    @Foo
    @Receiver
    void event(MyCompoundEvent evt) {
      this.evt = evt;
      latch.countDown();
    }
  }

  @Test(timeout = testDelay)
  public void test() throws InterruptedException {
    EventDispatch d = EventDispatchers.create();
    MyReceiver receiver = new MyReceiver();
    d.register(receiver);

    MyCompoundEvent evt = new MyCompoundEvent();
    d.fire(evt);
    receiver.latch.await();

    assertTrue(evt.asEventFacet(FooEvent.class).foo);
    assertTrue(evt.asEventFacet(BarEvent.class).bar);
  }
}
