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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

public class ShortCircuitTest {

  @EventDecoratorBinding(ADecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface A {}

  /**
   * Copies the dispatch status from the context into the event.
   */
  static class ADecorator implements EventDecorator<A, MyEvent> {

    @Override
    public Callable<Object> wrap(final EventDecorator.Context<A, MyEvent> ctx) {
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            return ctx.getWork().call();
          } finally {
            if (ctx.wasDispatched()) {
              ctx.getEvent().dispatched = true;
              ctx.getEvent().thrown = ctx.wasThrown();
            }
          }
        }
      };
    }
  }

  @EventDecoratorBinding(BDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface B {}

  /**
   * Calls one of the short-circuit methods or returns null.
   */
  static class BDecorator implements EventDecorator<B, MyEvent> {
    @Override
    public Callable<Object> wrap(final EventDecorator.Context<B, MyEvent> ctx) {
      if (ctx.getEvent().shouldShortCircuitFromWrap) {
        ctx.shortCircuit();
      }
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          if (ctx.getEvent().shouldThrow) {
            ctx.shortCircuit(new RuntimeException("Hello world!"));
          } else if (ctx.getEvent().shouldShortCircuitFromCallable) {
            ctx.shortCircuit();
          }
          // Never call the underlying work.
          return null;
        }
      };
    }
  }

  static class MyEvent implements Event {
    boolean dispatched;
    boolean shouldShortCircuitFromCallable;
    boolean shouldShortCircuitFromWrap;
    boolean shouldThrow;
    Throwable thrown;
  }

  @A
  static class MyReceiver {
    @B
    @Receiver(synchronous = true)
    void receive(MyEvent evt) {
      throw new RuntimeException("Should never see this");
    }
  }

  private EventDispatch d;

  @Before
  public void before() {
    d = EventDispatchers.create();
    d.register(MyReceiver.class);
  }

  @Test
  public void testNull() {
    MyEvent evt = new MyEvent();

    d.fire(evt);

    assertFalse(evt.dispatched);
    assertNull(evt.thrown);
  }

  @Test
  public void testShortCircuit() {
    MyEvent evt = new MyEvent();
    evt.shouldShortCircuitFromCallable = true;

    d.fire(evt);

    assertTrue(evt.dispatched);
    assertNull(evt.thrown);
  }

  @Test
  public void testShortCircuitThrowable() {
    MyEvent evt = new MyEvent();
    evt.shouldThrow = true;

    d.fire(evt);

    assertTrue(evt.dispatched);
    assertNotNull(evt.thrown);
    assertEquals("Hello world!", evt.thrown.getMessage());
  }

  @Test
  public void testShortCircuitWrap() {
    MyEvent evt = new MyEvent();
    evt.shouldShortCircuitFromWrap = true;

    d.fire(evt);

    assertFalse(evt.dispatched);
    assertNull(evt.thrown);
  }
}
