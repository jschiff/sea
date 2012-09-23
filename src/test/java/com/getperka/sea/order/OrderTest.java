package com.getperka.sea.order;

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
import static org.junit.Assert.assertSame;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.OutcomeEvent;
import com.getperka.sea.decoration.OutcomeEvent.Implementation;
import com.getperka.sea.decoration.OutcomeEvent.Success;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

public class OrderTest {

  static class ListReceiver {
    private final CountDownLatch latch;
    private final List<MyEvent> received = new ArrayList<MyEvent>();
    private final List<MyEvent> success = new ArrayList<MyEvent>();

    ListReceiver(CountDownLatch latch) {
      this.latch = latch;
    }

    @Ordered
    @Receiver
    @Implementation
    void receive(MyEvent event) throws InterruptedException {
      synchronized (received) {
        received.add(event);
      }
      Thread.sleep(event.value);
    }

    @Ordered
    @Receiver
    @Success
    void success(MyEvent event) throws InterruptedException {
      synchronized (success) {
        success.add(event);
      }
      latch.countDown();
    }
  }

  static class MyEvent extends OutcomeEvent.Base {
    final int value;

    MyEvent(int value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return String.valueOf(isSuccess() + " " + value);
    }
  }

  @EventDecoratorBinding(RefiredDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Refired {}

  /**
   * Randomly re-dispatch events rather than calling through to the work.
   */
  @Singleton
  @Refired
  static class RefiredDecorator implements EventDecorator<Refired, Event> {
    @Inject
    EventDispatch dispatch;

    private final Random random = new Random(0);

    @Override
    public Callable<Object> wrap(final Context<Refired, Event> ctx) {
      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          if (random.nextBoolean()) {
            dispatch.fire(ctx.getEvent());
            // Fire twice, just for more stress
            dispatch.fire(ctx.getEvent());
            return null;
          }
          return ctx.getWork().call();
        }
      };
    }

  }

  static final int timeout = 100000;

  OrderedDispatch batch;
  EventDispatch dispatch;
  CountDownLatch latch;
  ListReceiver receiver;

  @After
  public void after() {
    dispatch.shutdown();
  }

  @Before
  public void before() {
    dispatch = EventDispatchers.create();
    batch = OrderedDispatchers.create(dispatch);
    latch = new CountDownLatch(10);
    receiver = new ListReceiver(latch);
    dispatch.register(receiver);
  }

  @Test(timeout = timeout)
  public void test() throws InterruptedException {
    List<MyEvent> events = makeEvents();
    batch.fire(events);
    latch.await();
    assertEquals(events, receiver.received);
  }

  @Test(timeout = timeout)
  public void testInterference() throws InterruptedException {
    dispatch.addGlobalDecorator(RefiredDecorator.class);
    List<MyEvent> events = makeEvents();
    batch.fire(events);
    latch.await();
    assertEquals(events, receiver.received);
  }

  @Test(timeout = timeout)
  public void testReverse() throws InterruptedException {
    List<MyEvent> events = makeEvents();
    Collections.reverse(events);
    batch.fire(events);
    latch.await();
    assertEquals(events, receiver.received);
  }

  @Test
  public void testSameness() {
    assertSame(batch, OrderedDispatchers.create(dispatch));
  }

  private List<MyEvent> makeEvents() {
    List<MyEvent> events = new ArrayList<MyEvent>();
    for (int i = 0, j = (int) latch.getCount(); i < j; i++) {
      events.add(new MyEvent(i));
    }
    return events;
  }
}
