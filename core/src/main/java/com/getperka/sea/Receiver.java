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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a receiver for an event. The annotated method must have a single parameter
 * assignable to {@link Event}. The method may have any any access modifier and may throw checked
 * exceptions. The name of the method is arbitrary.
 * 
 * <pre>
 * public class Foo {
 *   &#064;Receiver
 *   void onBar(BarEvent event) {}
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Receiver {
  /**
   * If {@code true}, the receiver method will be invoked during the call to
   * {@link EventDispatch#fire(Event)}, rather than on a separate thread. This is appropriate for
   * event receivers that need to inherit some state (such as an EntityManager) from the
   * thread-local context in which the event is being fired.
   */
  boolean synchronous() default false;
}
