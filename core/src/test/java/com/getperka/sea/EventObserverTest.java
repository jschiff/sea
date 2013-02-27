package com.getperka.sea;

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
import static org.junit.Assert.assertSame;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.getperka.sea.EventObserverTest.DropOther;
import com.getperka.sea.EventObserverTest.Filtered;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.ext.EventObserverBinding;
import com.getperka.sea.util.EventLatch;

/**
 * Smoke test for observer wiring.
 */
@DropOther
@Filtered
public class EventObserverTest {
  @EventObserverBinding(MyOtherEventFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface DropOther {}

  @EventObserverBinding(MyFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Filtered {
    String value() default "Hello World!";
  }

  static class MyEvent implements Event {
    boolean drop;
  }

  static class MyFilter implements EventObserver<Filtered, MyEvent> {
    @Override
    public void initialize(Filtered annotation) {
      assertEquals("Hello World!", annotation.value());
    }

    @Override
    public void observeEvent(Context<MyEvent> context) {
      assertEquals(Boolean.TRUE, context.getContext().getUserObject());
      assertSame(context.getEvent(), context.getOriginalEvent());
      if (context.getEvent().drop) {
        context.suppressEvent();
      }
    }

    @Override
    public void shutdown() {}
  }

  static class MyOtherEvent implements Event {}

  static class MyOtherEventFilter implements EventObserver<DropOther, MyOtherEvent> {
    @Override
    public void initialize(DropOther annotation) {}

    @Override
    public void observeEvent(Context<MyOtherEvent> context) {
      context.suppressEvent();
    }

    @Override
    public void shutdown() {}
  }

  @Test
  public void testSuppression() {
    EventDispatch dispatch = EventDispatchers.create(Executors.newSingleThreadExecutor());
    dispatch.addGlobalDecorator(getClass());

    MyEvent dropped = new MyEvent();
    dropped.drop = true;
    MyEvent sent = new MyEvent();

    EventLatch<MyEvent> latch = EventLatch.create(dispatch, MyEvent.class, 1);

    dispatch.fire(new MyOtherEvent(), false);
    dispatch.fire(dropped, true);
    dispatch.fire(sent, true);

    latch.awaitUninterruptibly();
    assertEquals(1, latch.getEventQueue().size());
    assertSame(sent, latch.getEventQueue().poll());
  }
}
