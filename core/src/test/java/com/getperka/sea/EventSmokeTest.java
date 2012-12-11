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

import static com.getperka.sea.TestConstants.testDelay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
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
import com.getperka.sea.decoration.Logged.Level;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

@Logged
public class EventSmokeTest {

  static class BuggyReceiver {
    @Logged(level = Level.INFO, value = "The following exception is expected")
    @Receiver
    void receive(Event e) {
      throw new RuntimeException("Should not prevent work from succeeding");
    }
  }

  static class Decorator implements EventDecorator<NeedsDecoration, MyEvent> {

    @Override
    public Callable<Object> wrap(final Context<NeedsDecoration, MyEvent> ctx) {
      assertNotNull(ctx.getAnnotation());
      assertEquals(CONTEXT_VALUE, ctx.getContext());
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

  private static final String CONTEXT_VALUE = "Hello Context!";

  private static volatile boolean decoratorCalled;
  private static final Queue<Event> received = new ConcurrentLinkedQueue<Event>();
  private static volatile CountDownLatch latch;

  private final EventDispatch dispatch = EventDispatchers.create();

  @Before
  public void before() {
    decoratorCalled = false;
    latch = null;
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
    latch = new CountDownLatch(1);
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
      dispatch.fire(new MyEvent(), CONTEXT_VALUE);
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
      dispatch.fire(new MyEvent(), CONTEXT_VALUE);
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
  public void testRegistration() throws InterruptedException {
    Registration registrationA = dispatch.register(StaticReceiver.class);
    Registration registrationB = dispatch.register(new InstanceReceiver());
    dispatch.register(new InstanceReceiver());
    test(new MyEvent(), 3);

    registrationA.cancel();
    registrationB.cancel();
    // Ensure double-cancel is harmless
    registrationB.cancel();

    // Make sure the remaining registration still fires
    test(new MyEvent(), 1);
  }

  /**
   * Ensure that registering a target twice doesn't cause multiple dispatch.
   */
  @Test(timeout = testDelay)
  public void testReregistration() throws InterruptedException {
    Registration registrationA = dispatch.register(StaticReceiver.class);
    Registration registrationB = dispatch.register(StaticReceiver.class);
    assertNotSame(registrationA, registrationB);

    test();

    // Verify that all registrations must be canceled in order to prevent dispatches
    registrationA.cancel();

    test();

    registrationB.cancel();

    test(new MyEvent(), 0);
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
    test(new MyEvent(), 1);
  }

  private void test(Event event, int count) throws InterruptedException {
    latch = new CountDownLatch(count);

    dispatch.fire(event, CONTEXT_VALUE);
    latch.await();
    assertEquals(count, received.size());
    while (!received.isEmpty()) {
      assertSame(event, received.poll());
    }
    assertTrue(decoratorCalled);
  }
}
