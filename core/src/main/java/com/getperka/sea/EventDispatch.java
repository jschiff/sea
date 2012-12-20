package com.getperka.sea;

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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import javax.inject.Provider;

import com.getperka.sea.ext.DecoratorOrder;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventObserver;

/**
 * This is the main interface for the Simple Event Architecture.
 * 
 * @see EventDispatchers#create()
 */
public interface EventDispatch {
  /**
   * Apply {@link EventDecorator} or {@link EventObserver} binding annotations on a {@link Class},
   * {@link Method}, or {@link Package} to all event dispatches.
   * <p>
   * For example, to enable simple logging of all events:
   * 
   * <pre>
   * &#064;Logged
   * public class MyApp {
   *   public static void main(String[] args) {
   *     EventDispatch dispatch = EventDispatcher.create();
   *     dispatch.addGlobalDecorator(MyApp.class);
   *     // Do work
   *   }
   * }
   * </pre>
   * 
   * The order in which decorators or observers are applied is undefined unless a
   * {@link DecoratorOrder} annotation is also present. Multiple invocations of this method register
   * progressively "more global" decorations.
   */
  void addGlobalDecorator(AnnotatedElement element);

  /**
   * Asynchronously dispatch an {@link Event}.
   * 
   * @param event the Event to dispatch. {@code null} values will be ignored
   */
  void fire(Event event);

  /**
   * Asynchronously dispatch an {@link Event}. This method accepts an optional per-invocation
   * context object which is passed along to event decorators and may be made available to event
   * receivers. This context object can be used for any form of signaling, such as event-loop
   * detection.
   * 
   * @param event the Event to dispatch. {@code null} values will be ignored
   * @param context an arbitrary object to associate with the specific call to {@code fire}
   * @see EventDecorator.Context#getContext()
   */
  void fire(Event event, Object context);

  /**
   * Register a receiver class. Instances of the class will be created on demand for each event the
   * class receives. Only {@link Receiver} methods declared in the class will be registered.
   * 
   * @throws BadReceiverException if an unsatisfactory {@code @Receiver} declaration is encountered
   */
  Registration register(Class<?> receiver) throws BadReceiverException;

  /**
   * Register a receiver class, using the given Provider to instantiate the instances. Only
   * {@link Receiver} methods declared in the class will be registered.
   * 
   * @throws BadReceiverException if an unsatisfactory {@code @Receiver} declaration is encountered
   */
  <T> Registration register(Class<T> receiver, Provider<? extends T> provider)
      throws BadReceiverException;

  /**
   * Register a singleton receiver. Only {@link Receiver} methods declared in the object's class
   * will be registered.
   * 
   * @throws BadReceiverException if an unsatisfactory {@code @Receiver} declaration is encountered
   */
  Registration register(Object receiver) throws BadReceiverException;

  /**
   * Prevents any further events from being dispatched. Events that are queued will be dropped.
   * Events currently being dispatched will be allowed to continue.
   */
  void shutdown();
}
