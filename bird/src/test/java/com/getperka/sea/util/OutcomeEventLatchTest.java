package com.getperka.sea.util;
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
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.BaseOutcomeEvent;
import com.getperka.sea.decoration.Implementation;

public class OutcomeEventLatchTest {
  static class MyEvent extends BaseOutcomeEvent {
    String data;
    boolean fail;
  }

  static class MyReceiver {
    @Implementation
    @Receiver
    void receive(MyEvent evt) {
      if (evt.fail) {
        throw new RuntimeException("EXPECTED");
      }
      evt.data = "Hello World!";
    }
  }

  @Test
  public void testFailure() {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(MyReceiver.class);

    MyEvent evt = new MyEvent();
    evt.fail = true;
    OutcomeEventLatch.create(dispatch).awaitOutcome(evt, 1, TimeUnit.SECONDS);
    assertNotNull(evt.getFailure());
  }

  @Test
  public void testSuccess() {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(MyReceiver.class);

    MyEvent evt = new MyEvent();
    OutcomeEventLatch.create(dispatch).awaitOutcome(evt, 1, TimeUnit.SECONDS);
    assertEquals("Hello World!", evt.data);
  }
}
