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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.getperka.sea.decoration.Logged;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

@Logged
public class EventSmokeTest {

  static class BuggyReceiver {
    @Logged(info = "The following exception is expected")
    @Receiver
    void receive(Event e) {
      throw new RuntimeException("Should not prevent work from succeeding");
    }
  }

  static class Decorator implements EventDecorator<NeedsDecoration, MyEvent> {
    @Override
    public Class<? extends NeedsDecoration> getAnnotationType() {
      return NeedsDecoration.class;
    }

    @Override
    public Class<? extends MyEvent> getEventType() {
      return MyEvent.class;
    }

    @Override
    public Callable<Object> wrap(final Context<NeedsDecoration, MyEvent> ctx) {
      assertNotNull(ctx.getAnnotation());
      assertNotNull(ctx.getEvent());
      assertNotNull(ctx.getWork());
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          decoratorCalled = true;
          return ctx.getWork().call();
        }
      };
    }
  }

  @NeedsDecoration
  static class InstanceReceiver {
    @Receiver
    void receive(MyEvent event) {
      received.add(event);
      latch.countDown();
    }

    @Receiver
    void receive(OtherEvent event) {
      received.add(event);
      latch.countDown();
    }

    @Receiver
    void shouldNotReceive() {
      fail();
    }

    // Not decorated with @Receiver
    void shouldNotReceive(MyEvent event) {
      fail();
    }

    @Receiver
    void shouldNotReceive(MyEvent event, boolean ignored) {
      fail();
    }

    @Receiver
    void shouldNotReceive(String ignored) {
      fail();
    }
  }

  @EventDecoratorBinding(Decorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
  @interface NeedsDecoration {}

  @Nullify
  static class NullifiedReceiver {
    @Receiver
    void receive(Event e) {
      fail();
    }
  }

  @EventDecoratorBinding(NullifyingDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Nullify {
    boolean value() default true;
  }

  static class NullifyingDecorator implements EventDecorator<Nullify, Event> {
    @Override
    public Class<? extends Nullify> getAnnotationType() {
      return Nullify.class;
    }

    @Override
    public Class<? extends Event> getEventType() {
      return Event.class;
    }

    @Override
    public Callable<Object> wrap(Context<Nullify, Event> ctx) {
      decoratorCalled = true;
      received.add(ctx.getEvent());
      latch.countDown();
      return ctx.getAnnotation().value() ? null : ctx.getWork();
    }
  }

  static class OtherEvent implements Event {}

  static class ShutdownReceiver {
    @Inject
    EventDispatch events;

    @NeedsDecoration
    @Receiver
    void receive(MyEvent event) {
      events.shutdown();
      received.add(event);
      latch.countDown();
      // If the shutdown fails, this method will just be called again
      events.fire(event);
    }
  }

  static class StaticReceiver {
    @NeedsDecoration
    @Receiver
    static void receiveStatic(MyEvent event) {
      received.add(event);
      latch.countDown();
    }

    private StaticReceiver() {
      fail("Should not attempt to instantiate a StaticReceiver");
    }
  }

  private static class MyEvent implements Event {}

  private static volatile boolean decoratorCalled;
  private static final Queue<Event> received = new ConcurrentLinkedQueue<Event>();
  private static volatile CountDownLatch latch;

  private final EventDispatch dispatch = EventDispatchers.create();

  private static final long testDelay = 100;

  @Before
  public void before() {
    decoratorCalled = false;
    latch = new CountDownLatch(1);
    received.clear();

    dispatch.addGlobalDecorator(getClass());
  }

  @Test(timeout = testDelay)
  public void testBuggyReceiver() {
    dispatch.register(BuggyReceiver.class);
    dispatch.fire(new MyEvent());
  }

  @Test(timeout = testDelay)
  public void testDecoratorIgnoresOtherEvent() throws InterruptedException {
    dispatch.register(InstanceReceiver.class);
    // Ensure double-registering is a no-op
    dispatch.register(InstanceReceiver.class);
    dispatch.fire(new OtherEvent());
    latch.await();
    assertFalse(decoratorCalled);
  }

  @Test(timeout = testDelay)
  public void testExplicitInstance() throws InterruptedException {
    dispatch.register(new InstanceReceiver());
    test();
  }

  @Test(timeout = testDelay)
  public void testImplicitInstance() throws InterruptedException {
    dispatch.register(InstanceReceiver.class);
    test();
  }

  @Test(timeout = testDelay)
  public void testMultipleEventDispatch() throws InterruptedException {
    latch = new CountDownLatch(5);
    dispatch.register(InstanceReceiver.class);
    for (long i = 0, j = latch.getCount(); i < j; i++) {
      dispatch.fire(new MyEvent());
    }
    latch.await();
    assertEquals(5, received.size());
  }

  @Test(timeout = testDelay)
  public void testMultipleEventDispatchMultipleReceivers() throws InterruptedException {
    latch = new CountDownLatch(10);
    dispatch.register(InstanceReceiver.class);
    dispatch.register(StaticReceiver.class);
    dispatch.register(BuggyReceiver.class);
    for (long i = 0, j = 5; i < j; i++) {
      dispatch.fire(new MyEvent());
    }
    latch.await();
    assertEquals(10, received.size());
  }

  @Test(timeout = testDelay)
  public void testNoReceivers() {
    dispatch.fire(new Event() {});
  }

  @Test(timeout = testDelay)
  public void testNullification() throws InterruptedException {
    dispatch.register(NullifiedReceiver.class);
    test();
  }

  @Test(timeout = testDelay)
  public void testShutdown() throws InterruptedException {
    dispatch.register(ShutdownReceiver.class);
    test();
  }

  @Test(timeout = testDelay)
  public void testStatic() throws InterruptedException {
    dispatch.register(StaticReceiver.class);
    test();
  }

  private void test() throws InterruptedException {
    test(new MyEvent());
  }

  private void test(Event event) throws InterruptedException {
    dispatch.fire(event);
    latch.await();
    assertEquals(1, received.size());
    assertSame(event, received.poll());
    assertTrue(decoratorCalled);
  }
}
