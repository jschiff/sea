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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;

public class TaggedEventTest {
  static class MyEvent extends BaseTaggedEvent {}

  class MyReceiver {
    boolean all;
    boolean any;
    boolean instance;
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

    @Tagged(receiverInstance = true)
    @Receiver
    void instance(MyEvent evt) {
      instance = true;
      latch.countDown();
    }

    @Tagged(classes = TaggedEventTest.class, strings = markerString, receiverInstance = true,
        mode = TagMode.NONE)
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

  private EventDispatch dispatch = EventDispatchers.create();
  private CountDownLatch latch;

  @Test(timeout = testDelay)
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

  @Test(timeout = testDelay)
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

  @Test(timeout = testDelay)
  public void testInstance() throws InterruptedException {
    latch = new CountDownLatch(2);
    MyReceiver receiverA = new MyReceiver();
    MyReceiver receiverB = new MyReceiver();

    dispatch.register(receiverA);
    dispatch.register(receiverB);

    MyEvent evt = new MyEvent();
    evt.addTag(Tag.create(receiverB));

    dispatch.fire(evt);
    latch.await();

    assertFalse(receiverA.instance);
    assertTrue(receiverA.none);
    assertTrue(receiverB.instance);
    assertFalse(receiverB.none);
  }

  @Test(timeout = testDelay)
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
