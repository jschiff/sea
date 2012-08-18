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

  void fire(Event event);

  void register(Class<?> receiver);

  <T> void register(Class<T> receiver, Provider<? extends T> provider);

  void register(Object receiver);

  void shutdown();
}
