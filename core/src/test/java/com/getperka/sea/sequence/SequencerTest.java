package com.getperka.sea.sequence;

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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;

public class SequencerTest {
  static class MyEvent implements Event {
    private boolean fail;
    private String value;

    public String getValue() {
      return value;
    }

    public boolean isFail() {
      return fail;
    }

    public void setFail(boolean fail) {
      this.fail = fail;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  class MySequencer extends Sequencer<String> {
    private final MyEvent toSend;

    public MySequencer(MyEvent toSend) {
      this.toSend = toSend;
    }

    @Override
    protected void start() {
      assertTrue(isExecuting());
      fire(toSend);
    }

    void checkAfterCall() {
      assertFalse(isExecuting());

      // Check multiple fail / finish calls
      try {
        finish("Should not see this");
        Assert.fail();
      } catch (IllegalStateException expected) {}

      try {
        fail("Should not see this");
        Assert.fail();
      } catch (IllegalStateException expected) {}
    }

    @Receiver
    void onEvent(MyEvent evt) {
      assertTrue(isExecuting());
      assertSame(toSend, evt);
      if (evt.isFail()) {
        fail(evt.getValue());
      } else {
        finish(evt.getValue());
      }
    }
  }

  private final EventDispatch dispatch = EventDispatchers.create();

  @Test(timeout = testDelay)
  public void testFail() {
    MyEvent evt = new MyEvent();
    evt.setFail(true);
    evt.setValue("Goodbye World!");
    MySequencer seq = new MySequencer(evt);
    // Not constructed via injector, so the dispatch must be explicitly set
    seq.setEventDispatch(dispatch);

    try {
      seq.call();
      fail();
    } catch (SequenceFailureException e) {
      assertEquals("Goodbye World!", e.getMessage());
    }
    seq.checkAfterCall();
  }

  @Test(timeout = testDelay)
  public void testFinish() {
    MyEvent evt = new MyEvent();
    evt.setValue("Hello World!");
    MySequencer seq = new MySequencer(evt);
    // Not constructed via injector, so the dispatch must be explicitly set
    seq.setEventDispatch(dispatch);
    assertFalse(seq.isExecuting());
    assertEquals("Hello World!", seq.call());
    seq.checkAfterCall();
  }

  @Test
  public void testNoEventDispatch() {
    MySequencer seq = new MySequencer(null);
    try {
      seq.call();
      fail();
    } catch (IllegalStateException expected) {}
  }
}
