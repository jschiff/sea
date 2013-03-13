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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import com.getperka.sea.impl.ReceiverMethodInvocation;
import com.getperka.sea.util.EventLatch;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Verifies that injecting additional values into receiver methods works correctly.
 */
public class InjectionTest {

  @Retention(RetentionPolicy.RUNTIME)
  @interface IrrelevantAnnotation {}

  static class MyEvent implements Event {
    boolean ok;
  }

  @Test(timeout = TestConstants.testDelay)
  public void test() {
    EventDispatch dispatch = EventDispatchers.create();

    EventLatch<MyEvent> latch = new EventLatch<MyEvent>(dispatch, 1) {
      @Receiver
      void receiver(ReceiverMethodInvocation invocation, @IrrelevantAnnotation MyEvent evt) {
        evt.ok = this == invocation.getReceiverInstance();
        countDown(evt);
      }
    };
    dispatch.fire(new MyEvent());

    latch.awaitUninterruptibly();
    assertTrue(latch.getEventQueue().poll().ok);
  }

  @Test
  public void testBadReceiverDeclaration() {
    try {
      EventDispatchers.create().register(new Object() {
        @Receiver
        void badReceiver(Event event, boolean unbound) {}
      });
      fail();
    } catch (BadReceiverException expected) {}
  }

  /**
   * Test that bindings with {@link com.google.inject.name.Named} work.
   */
  @Test(timeout = TestConstants.testDelay)
  public void testGuiceNamed() {
    EventDispatch dispatch = EventDispatchers.create(new AbstractModule() {
      @Override
      protected void configure() {
        bindConstant().annotatedWith(Names.named("hello")).to("Hello World!");
      }
    });

    EventLatch<MyEvent> latch = new EventLatch<MyEvent>(dispatch, 1) {
      @Receiver
      void receiver(MyEvent evt, @com.google.inject.name.Named("hello") String hello) {
        evt.ok = "Hello World!".equals(hello);
        countDown(evt);
      }
    };
    dispatch.fire(new MyEvent());

    latch.awaitUninterruptibly();
    assertTrue(latch.getEventQueue().poll().ok);
  }

  /**
   * Test that bindings with {@link javax.inject.Named} work.
   */
  @Test(timeout = TestConstants.testDelay)
  public void testJavaxNamed() {
    EventDispatch dispatch = EventDispatchers.create(new AbstractModule() {
      @Override
      protected void configure() {
        bindConstant().annotatedWith(Names.named("hello")).to("Hello World!");
      }
    });

    EventLatch<MyEvent> latch = new EventLatch<MyEvent>(dispatch, 1) {
      @Receiver
      void receiver(MyEvent evt, @javax.inject.Named("hello") String hello) {
        evt.ok = "Hello World!".equals(hello);
        countDown(evt);
      }
    };
    dispatch.fire(new MyEvent());

    latch.awaitUninterruptibly();
    assertTrue(latch.getEventQueue().poll().ok);
  }
}
