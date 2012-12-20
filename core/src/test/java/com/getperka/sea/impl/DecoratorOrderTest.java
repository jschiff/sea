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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.decoration.Failure;
import com.getperka.sea.decoration.Logged;
import com.getperka.sea.decoration.Success;
import com.getperka.sea.decoration.Timed;
import com.getperka.sea.ext.DecoratorOrder;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.ext.EventObserverBinding;

public class DecoratorOrderTest {

  @Logged
  @Timed(value = 1)
  @ObserverA
  @ObserverB
  @DecoratorOrder({ Logged.class, Timed.class, ObserverA.class, ObserverB.class })
  interface A {}

  @Logged
  @Timed(value = 1)
  @ObserverA
  @ObserverB
  @DecoratorOrder({ Timed.class, Logged.class, ObserverB.class, ObserverA.class })
  interface B {}

  @Logged
  @Timed(value = 1)
  @ObserverA
  @ObserverB
  @DecoratorOrder({ Logged.class, ObserverA.class })
  interface C {}

  @Logged
  @Timed(value = 1)
  @ObserverA
  @ObserverB
  @DecoratorOrder({ Timed.class, ObserverB.class })
  interface D {}

  /**
   * Test one "extra" and an unused decorator.
   */
  @Logged
  @Timed(value = 1)
  @Success
  @DecoratorOrder({ Timed.class, Logged.class, Failure.class })
  interface E {}

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
    checkDecorators(A.class, Logged.class, Timed.class);
    checkObservers(A.class, ObserverA.class, ObserverB.class);
  }

  @Test
  public void testB() throws NoSuchMethodException {
    checkDecorators(B.class, Timed.class, Logged.class);
    checkObservers(B.class, ObserverB.class, ObserverA.class);
  }

  @Test
  public void testC() throws NoSuchMethodException {
    checkDecorators(C.class, Logged.class, Timed.class);
    checkObservers(C.class, ObserverA.class, ObserverB.class);
  }

  @Test
  public void testD() throws NoSuchMethodException {
    checkDecorators(D.class, Timed.class, Logged.class);
    checkObservers(D.class, ObserverB.class, ObserverA.class);
  }

  @Test
  public void testE() throws NoSuchMethodException {
    checkDecorators(E.class, Timed.class, Logged.class, Success.class);
  }

  void dummy() {}

  private void checkDecorators(Class<?> clazz, Class<?>... expectedOrder) {
    map.register(clazz);

    List<Annotation> annotations = new ArrayList<Annotation>();
    List<Provider<EventDecorator<Annotation, Event>>> providers = new ArrayList<Provider<EventDecorator<Annotation, Event>>>();

    map.computeDecorators(method, annotations, providers);
    // Reverse annotations, since the wrap() order is backwards
    Collections.reverse(annotations);

    assertEquals("Annotation count mismatch", expectedOrder.length, annotations.size());
    for (int i = 0, j = expectedOrder.length; i < j; i++) {
      assertEquals(expectedOrder[i], annotations.get(i).annotationType());
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
