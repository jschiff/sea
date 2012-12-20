package com.getperka.sea;

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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;

import org.junit.Test;

import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.ext.ExternalBinding;
import com.getperka.sea.ext.ExternalBindings;
import com.getperka.sea.util.EventLatch;

/**
 * Tests {@link ExternalBindings}.
 */
public class ExternalBindingsTest {

  @Retention(RetentionPolicy.RUNTIME)
  @interface External {}

  @ExternalBindings({
      @ExternalBinding(
          annotation = External.class,
          decorator = MyDecorator.class,
          observer = MyObserver.class),
      @ExternalBinding(annotation = Ignored.class)
  })
  @External
  @Ignored
  interface Global {}

  @Retention(RetentionPolicy.RUNTIME)
  @interface Ignored {}

  static class MyDecorator implements EventDecorator<External, MyEvent> {

    @Override
    public Callable<Object> wrap(EventDecorator.Context<External, MyEvent> ctx) {
      ctx.getEvent().decorator = true;
      return ctx.getWork();
    }
  }

  static class MyEvent implements Event {
    boolean decorator;
    boolean observer;
  }

  static class MyObserver implements EventObserver<External, MyEvent> {

    @Override
    public void initialize(External annotation) {
      assertNotNull(annotation);
    }

    @Override
    public void observeEvent(EventObserver.Context<MyEvent> context) {
      context.getEvent().observer = true;
    }

    @Override
    public void shutdown() {}
  }

  @Test(timeout = TestConstants.testDelay)
  public void test() {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.addGlobalDecorator(Global.class);

    EventLatch<MyEvent> latch = EventLatch.create(dispatch, MyEvent.class, 1);
    dispatch.fire(new MyEvent());
    latch.awaitUninterruptibly();
    MyEvent evt = latch.getEventQueue().poll();
    assertTrue(evt.decorator);
    assertTrue(evt.observer);
  }
}
