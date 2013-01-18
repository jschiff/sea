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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.TimedDecorator.TimeoutError;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

public class TimedDecoratorTest {

  class InterruptedReceiver {
    boolean interrupted;

    @Receiver
    @Timed(10)
    synchronized void sleep(Event event) {
      try {
        for (;;) {
          wait();
        }
      } catch (InterruptedException e) {
        interrupted = true;
      } finally {
        latch.countDown();
      }
    }
  }

  class OkReceiver {
    @Receiver
    @Timed(10)
    void noOp(Event event) {
      latch.countDown();
    }
  }

  static class SlowDecorator implements EventDecorator<SlowEvent, Event> {
    static CountDownLatch latch;

    @Override
    public Callable<Object> wrap(Context<SlowEvent, Event> ctx) {
      // Sanity check
      assertNotNull(latch);
      assertEquals(1, latch.getCount());

      return new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            doWait();
          } catch (TimeoutError e) {
            latch.countDown();
            TimedDecoratorTest.<RuntimeException> sneakyThrow(e);
          }
          return null;
        }
      };
    }

    synchronized void doWait() throws TimeoutError, InterruptedException {
      for (;;) {
        wait();
      }
    }

  }

  @EventDecoratorBinding(SlowDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface SlowEvent {}

  class StoppedReceiver {
    TimeoutError caught;

    void doWait() throws TimeoutError, InterruptedException {
      for (;;) {
        wait();
      }
    }

    @Receiver
    @Timed(value = 10, stop = true)
    synchronized void sleep(Event event) {
      try {
        doWait();
      } catch (InterruptedException e) {
        fail();
      } catch (TimeoutError e) {
        /*
         * Because TimeoutError isn't publicly visible, this kind of catch block can't normally be
         * written. The error is re-thrown to mimic the behavior of code that is actually killed.
         */
        caught = e;
        TimedDecoratorTest.<RuntimeException> sneakyThrow(e);
      } finally {
        latch.countDown();
      }
    }

  }

  @Timed(value = 10, stop = true)
  class StoppedReceiverWithSlowEvent {
    @Receiver
    @SlowEvent
    void noOp(Event event) {}
  }

  @SuppressWarnings("unchecked")
  static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }

  private final EventDispatch dispatch = EventDispatchers.create();
  private final CountDownLatch latch = new CountDownLatch(1);

  @Test(timeout = testDelay)
  public void testInterrupt() throws InterruptedException {
    InterruptedReceiver receiver = new InterruptedReceiver();
    dispatch.register(receiver);
    dispatch.fire(new Event() {});
    latch.await();
    assertTrue(receiver.interrupted);
  }

  @Test(timeout = testDelay)
  public void testOk() throws InterruptedException {
    OkReceiver receiver = new OkReceiver();
    dispatch.register(receiver);
    dispatch.fire(new Event() {});
    latch.await();
  }

  @Test(timeout = testDelay)
  public void testStop() throws InterruptedException {
    StoppedReceiver receiver = new StoppedReceiver();
    dispatch.register(receiver);
    dispatch.fire(new Event() {});
    latch.await();
    assertNotNull(receiver.caught);
  }

  @Test(timeout = testDelay)
  public void testStopInSlowDecorator() throws InterruptedException {
    SlowDecorator.latch = latch;
    StoppedReceiverWithSlowEvent receiver = new StoppedReceiverWithSlowEvent();
    dispatch.register(receiver);
    dispatch.fire(new Event() {});
    latch.await();
  }
}
