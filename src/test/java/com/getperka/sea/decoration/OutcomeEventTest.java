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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.OutcomeEvent.Failure;
import com.getperka.sea.decoration.OutcomeEvent.Implementation;
import com.getperka.sea.decoration.OutcomeEvent.Success;

public class OutcomeEventTest {
  static class MyEvent extends OutcomeEvent.Base {
    private int value;

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }

  class MyReceiver {
    int value;
    Throwable thrown;

    @Failure
    @Receiver
    void failure(MyEvent evt) {
      thrown = evt.getFailure();
      latch.countDown();
    }

    @Implementation
    @Receiver
    void implementation(MyEvent evt) {
      if (evt.getValue() < 0) {
        throw new IllegalArgumentException();
      }
      evt.setValue(evt.getValue() + 1);
    }

    @Success
    @Receiver
    void success(MyEvent evt) {
      value = evt.getValue();
      latch.countDown();
    }
  }

  private static final int testTimeout = 100000;
  private EventDispatch dispatch = EventDispatchers.create();
  private CountDownLatch latch = new CountDownLatch(1);

  @Test(timeout = testTimeout)
  public void testFailure() throws InterruptedException {
    MyReceiver r = new MyReceiver();
    dispatch.register(r);

    MyEvent evt = new MyEvent();
    evt.setValue(-1);
    dispatch.fire(evt);
    latch.await();

    assertNotNull(r.thrown);
  }

  @Test(timeout = testTimeout)
  public void testSuccess() throws InterruptedException {
    MyReceiver r = new MyReceiver();
    dispatch.register(r);

    MyEvent evt = new MyEvent();
    dispatch.fire(evt);

    latch.await();

    assertEquals(1, evt.value);
  }
}
