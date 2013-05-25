package com.getperka.sea;

/*
 * #%L
 * Simple Event Architecture - Core
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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
import static org.junit.Assert.assertNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import org.junit.Test;

import com.getperka.sea.RepeatedDecoratorTest.Logged;
import com.getperka.sea.RepeatedDecoratorTest.Repeated;
import com.getperka.sea.ext.DecoratorOrder;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.impl.DecoratorMap;
import com.getperka.sea.impl.DecoratorMap.DecoratorInfo;
import com.getperka.sea.impl.HasInjector;

/**
 * Verifies the behavior when a decorator annotation is repeated in the decorator chain.
 */
@Logged
@Repeated(value = "outer")
@DecoratorOrder({ Logged.class, Repeated.class })
public class RepeatedDecoratorTest {

  @EventDecoratorBinding(RepeatedDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Logged {}

  static class MyEvent implements Event {
    String value;
  }

  @Logged
  @Repeated(value = "middle")
  static class MyReceiver {
    SynchronousQueue<MyEvent> queue = new SynchronousQueue<RepeatedDecoratorTest.MyEvent>();

    @Receiver
    @Repeated(value = "inner")
    void receive(MyEvent evt) throws InterruptedException {
      queue.put(evt);
    }
  }

  @EventDecoratorBinding(RepeatedDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Repeated {
    String value();
  }

  static class RepeatedDecorator implements EventDecorator<Repeated, MyEvent> {

    @Override
    public Callable<Object> wrap(EventDecorator.Context<Repeated, MyEvent> ctx) {
      assertNull(ctx.getEvent().value);
      ctx.getEvent().value = ctx.getAnnotation().value();
      return ctx.getWork();
    }
  }

  @Test
  public void test() throws InterruptedException {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.addGlobalDecorator(getClass());

    MyReceiver r = new MyReceiver();
    dispatch.register(r);

    dispatch.fire(new MyEvent());

    MyEvent fired = r.queue.take();
    assertEquals("inner", fired.value);
  }

  @Test
  public void testMap() throws SecurityException, NoSuchMethodException {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.addGlobalDecorator(getClass());

    DecoratorMap map = ((HasInjector) dispatch).getInjector().getInstance(DecoratorMap.class);

    Method method = MyReceiver.class.getDeclaredMethod("receive", MyEvent.class);
    List<DecoratorInfo> info = map.getDecoratorInfo(method);
    assertEquals(2, info.size());

    // The info appears in reverse order, since the outermost decoration must be applied last
    assertEquals(Logged.class, info.get(1).getAnnotation().annotationType());
    assertEquals(Repeated.class, info.get(0).getAnnotation().annotationType());
    assertEquals("inner", ((Repeated) info.get(0).getAnnotation()).value());
  }
}
