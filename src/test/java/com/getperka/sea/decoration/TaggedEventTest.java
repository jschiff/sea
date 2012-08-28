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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.TaggedEvent.Tag;
import com.getperka.sea.decoration.TaggedEvent.TagMode;
import com.getperka.sea.decoration.TaggedEvent.Tagged;

public class TaggedEventTest {
  static class MyEvent extends TaggedEvent.Base {}

  class MyReceiver {
    boolean all;
    boolean any;
    boolean none;

    @Tagged(classes = TaggedEventTest.class, strings = markerString, mode = TagMode.ALL)
    @Receiver
    void all(MyEvent evt) {
      all = true;
      latch.countDown();
    }

    @Tagged(classes = TaggedEventTest.class, strings = markerString, mode = TagMode.ANY)
    @Receiver
    void any(MyEvent evt) {
      any = true;
      latch.countDown();
    }

    @Tagged(classes = TaggedEventTest.class, strings = markerString, mode = TagMode.NONE)
    @Receiver
    void none(MyEvent evt) {
      none = true;
      latch.countDown();
    }
  }

  static final Class<?> markerClass = TaggedEventTest.class;
  static final String markerString = "TaggedEvent";
  static final Tag tagClass = Tag.create(markerClass);
  static final Tag tagString = Tag.create(markerString);
  static final int testTimeout = 100;

  private EventDispatch dispatch = EventDispatchers.create();
  private CountDownLatch latch;

  @Test(timeout = testTimeout)
  public void testAll() throws InterruptedException {
    latch = new CountDownLatch(2);
    MyReceiver receiver = new MyReceiver();
    dispatch.register(receiver);

    MyEvent evt = new MyEvent();
    evt.addTag(tagClass);
    evt.addTag(tagString);
    dispatch.fire(evt);
    latch.await();

    assertTrue(receiver.all);
    assertTrue(receiver.any);
    assertFalse(receiver.none);
  }

  @Test(timeout = testTimeout)
  public void testAny() throws InterruptedException {
    latch = new CountDownLatch(1);
    MyReceiver receiver = new MyReceiver();
    dispatch.register(receiver);

    MyEvent evt = new MyEvent();
    evt.addTag(tagClass);
    dispatch.fire(evt);
    latch.await();

    assertFalse(receiver.all);
    assertTrue(receiver.any);
    assertFalse(receiver.none);
  }

  @Test(timeout = testTimeout)
  public void testNone() throws InterruptedException {
    latch = new CountDownLatch(1);
    MyReceiver receiver = new MyReceiver();
    dispatch.register(receiver);

    MyEvent evt = new MyEvent();
    dispatch.fire(evt);
    latch.await();

    assertFalse(receiver.all);
    assertFalse(receiver.any);
    assertTrue(receiver.none);
  }
}
