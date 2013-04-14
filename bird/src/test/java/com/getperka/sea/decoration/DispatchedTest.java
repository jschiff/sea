package com.getperka.sea.decoration;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.Dispatched;
import com.getperka.sea.decoration.Logged;
import com.getperka.sea.decoration.Logged.Level;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

public class DispatchedTest {
  @EventDecoratorBinding(FilterImpl.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Filter {}

  static class FilterImpl implements EventDecorator<Filter, MyEvent> {
    @Override
    public Callable<Object> wrap(Context<Filter, MyEvent> ctx) {
      return ctx.getEvent().drop ? null : ctx.getWork();
    }
  }

  @Logged(exceptionLevel = Level.ERROR)
  class MyReceiver {
    private boolean alwaysCalled;
    private MyEvent expected;
    private boolean onlyRecevied;
    private boolean onlyUnrecevied;

    @Receiver
    @Dispatched(eventType = MyEvent.class)
    void alwaysCalled(DispatchCompleteEvent evt) {
      alwaysCalled = true;
      if (expected != null) {
        assertSame(expected, evt.getSource());
        assertEquals(EXPECTED_CONTEXT, evt.getContext().getUserObject());

        for (DispatchResult res : evt.getResults()) {
          assertSame(expected, res.getEvent());
          assertEquals("Hello World!", res.getReturnValue());
          assertNotNull(res.getTarget());
          assertNull(res.getThrown());
        }
      } else {
        for (DispatchResult res : evt.getResults()) {
          assertFalse(res.wasReceived());
        }
      }
      latch.countDown();
    }

    @Filter
    @Receiver
    String myEvent(MyEvent evt) {
      expected = evt;
      return "Hello World!";
    }

    @Receiver
    @Dispatched(onlyReceived = true)
    void onlyReceived(DispatchCompleteEvent evt) {
      onlyRecevied = true;
      assertSame(expected, evt.getSource());
      assertTrue(evt.wasReceived());
      latch.countDown();
    }

    @Receiver
    @Dispatched(onlyUnreceived = true)
    void onlyUnreceived(DispatchCompleteEvent evt) {
      onlyUnrecevied = true;
      assertNull(expected);
      assertFalse(evt.wasReceived());
      latch.countDown();
    }
  }

  private static class FooEvent implements Event {}

  private static class MyEvent implements Event {
    boolean drop;
  }

  private static final String EXPECTED_CONTEXT = "Hello Context!";

  private EventDispatch dispatch = EventDispatchers.create();
  private CountDownLatch latch;

  private MyReceiver receiver = new MyReceiver();

  @Test(timeout = testDelay)
  public void testOther() throws InterruptedException {
    latch = new CountDownLatch(1);
    dispatch.register(receiver);

    FooEvent evt = new FooEvent();
    dispatch.fire(evt, EXPECTED_CONTEXT);

    latch.await();

    assertNull(receiver.expected);
    assertFalse(receiver.alwaysCalled);
    assertFalse(receiver.onlyRecevied);
    assertTrue(receiver.onlyUnrecevied);
  }

  @Test(timeout = testDelay)
  public void testReceived() throws InterruptedException {
    latch = new CountDownLatch(2);
    dispatch.register(receiver);

    MyEvent evt = new MyEvent();
    dispatch.fire(evt, EXPECTED_CONTEXT);

    latch.await();

    assertSame(evt, receiver.expected);
    assertTrue(receiver.alwaysCalled);
    assertTrue(receiver.onlyRecevied);
    assertFalse(receiver.onlyUnrecevied);
  }

  @Test(timeout = testDelay)
  public void testUnreceived() throws InterruptedException {
    latch = new CountDownLatch(2);
    dispatch.register(receiver);

    MyEvent evt = new MyEvent();
    evt.drop = true;
    dispatch.fire(evt, EXPECTED_CONTEXT);

    latch.await();

    assertNull(receiver.expected);
    assertTrue(receiver.alwaysCalled);
    assertFalse(receiver.onlyRecevied);
    assertTrue(receiver.onlyUnrecevied);
  }
}
