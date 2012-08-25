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

import com.getperka.sea.ext.EventDecorator;

/**
 * This is the main interface for the Simple Event Architecture.
 * 
 * @see EventDispatchers#create()
 */
public interface EventDispatch {
  /**
   * Apply {@link EventDecorator} binding annotations on a {@link Class} or {@link Method} to all
   * event dispatches.
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
   */
  void addGlobalDecorator(AnnotatedElement element);

  /**
   * Asynchronously dispatch an {@link Event}. The actual event dispatch will typically occur on a
   * separate thread.
   */
  void fire(Event event);

  /**
   * Register a receiver class. Instances of the class will be created on demand for each event the
   * class receives.
   */
  void register(Class<?> receiver);

  /**
   * Register a receiver class, using the given Provider to instantiate the instances.
   */
  <T> void register(Class<T> receiver, Provider<? extends T> provider);

  /**
   * Register a singleton receiver.
   */
  void register(Object receiver);

  /**
   * Prevents any further events from being dispatched. Events that are queued will be dropped.
   * Events currently being dispatched will be allowed to continue.
   */
  void shutdown();
}
