package com.getperka.sea.impl;

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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.ext.DecoratorOrder;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.ext.EventObserverBinding;
import com.getperka.sea.impl.DecoratorMap.DecoratorInfo;

public class DecoratorOrderTest {

  @DecoratorB
  @DecoratorA
  @ObserverA
  @ObserverB
  @DecoratorOrder({ DecoratorB.class, DecoratorA.class, ObserverA.class, ObserverB.class })
  interface A {}

  @DecoratorB
  @DecoratorA
  @ObserverA
  @ObserverB
  @DecoratorOrder({ DecoratorA.class, DecoratorB.class, ObserverB.class, ObserverA.class })
  interface B {}

  @DecoratorB
  @DecoratorA
  @ObserverA
  @ObserverB
  @DecoratorOrder({ DecoratorB.class, ObserverA.class })
  interface C {}

  @DecoratorB
  @DecoratorA
  @ObserverA
  @ObserverB
  @DecoratorOrder({ DecoratorA.class, ObserverB.class })
  interface D {}

  @EventDecoratorBinding(MyDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface DecoratorA {}

  @EventDecoratorBinding(MyDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface DecoratorB {}

  @EventDecoratorBinding(MyDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface DecoratorC {}

  @EventDecoratorBinding(MyDecorator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface DecoratorD {}

  /**
   * Test one "extra" and an unused decorator.
   */
  @DecoratorB
  @DecoratorA
  @DecoratorD
  @DecoratorOrder({ DecoratorA.class, DecoratorB.class, DecoratorC.class })
  interface E {}

  static class MyDecorator implements EventDecorator<Annotation, Event> {
    @Override
    public Callable<Object> wrap(com.getperka.sea.ext.EventDecorator.Context<Annotation, Event> ctx) {
      return ctx.getWork();
    }
  }

  static class MyObserver implements EventObserver<Annotation, Event> {
    @Override
    public void initialize(Annotation annotation) {}

    @Override
    public void observeEvent(EventObserver.Context<Event> context) {}

    @Override
    public void shutdown() {}
  }

  @Retention(RetentionPolicy.RUNTIME)
  @EventObserverBinding(MyObserver.class)
  @interface ObserverA {}

  @Retention(RetentionPolicy.RUNTIME)
  @EventObserverBinding(MyObserver.class)
  @interface ObserverB {}

  private EventDispatch dispatch;
  private DecoratorMap map;
  private Method method;
  private ObserverMap observers;

  @Before
  public void before() throws NoSuchMethodException {
    dispatch = EventDispatchers.create();
    map = ((HasInjector) dispatch).getInjector().getInstance(DecoratorMap.class);
    method = getClass().getDeclaredMethod("dummy");
    observers = ((HasInjector) dispatch).getInjector().getInstance(ObserverMap.class);
  }

  @Test
  public void testA() throws NoSuchMethodException {
    checkDecorators(A.class, DecoratorB.class, DecoratorA.class);
    checkObservers(A.class, ObserverA.class, ObserverB.class);
  }

  @Test
  public void testB() throws NoSuchMethodException {
    checkDecorators(B.class, DecoratorA.class, DecoratorB.class);
    checkObservers(B.class, ObserverB.class, ObserverA.class);
  }

  @Test
  public void testC() throws NoSuchMethodException {
    checkDecorators(C.class, DecoratorB.class, DecoratorA.class);
    checkObservers(C.class, ObserverA.class, ObserverB.class);
  }

  @Test
  public void testD() throws NoSuchMethodException {
    checkDecorators(D.class, DecoratorA.class, DecoratorB.class);
    checkObservers(D.class, ObserverB.class, ObserverA.class);
  }

  @Test
  public void testE() throws NoSuchMethodException {
    checkDecorators(E.class, DecoratorA.class, DecoratorB.class, DecoratorD.class);
  }

  void dummy() {}

  private void checkDecorators(Class<?> clazz, Class<?>... expectedOrder) {
    map.register(clazz);

    List<DecoratorInfo> info = map.getDecoratorInfo(method);

    assertEquals("Annotation count mismatch", expectedOrder.length, info.size());
    for (int i = 0, j = expectedOrder.length; i < j; i++) {
      // Reverse order, since the wrap() order is backwards
      assertEquals(expectedOrder[i], info.get(j - i - 1).getAnnotation().annotationType());
    }
  }

  private void checkObservers(Class<?> clazz, Class<?>... expectedOrder) {
    observers.register(clazz);

    List<Annotation> annotations = observers.getAnnotations();
    assertEquals("Annotation count mismatch", expectedOrder.length, annotations.size());
    for (int i = 0, j = expectedOrder.length; i < j; i++) {
      assertEquals(expectedOrder[i], annotations.get(i).annotationType());
    }
  }
}
