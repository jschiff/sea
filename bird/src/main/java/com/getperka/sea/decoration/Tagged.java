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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * A receiver that should receive a {@link TaggedEvent} only if its tags match the
 */
@Documented
@EventDecoratorBinding(TaggedFilter.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface Tagged {
  /**
   * Match using class-based tags.
   */
  Class<?>[] classes() default {};

  /**
   * The matching mode to use. Defaults to {@link TagMode#ALL}.
   */
  TagMode mode() default TagMode.ALL;

  /**
   * Match using the instance of the receiver. Only events with a tag created using
   * {@link Tag#create(Object)} referencing the receiver instance will match. This matching mode
   * is especially useful when events are re-fired after being processed.
   * 
   * @see OutcomeEvent
   */
  boolean receiverInstance() default false;

  /**
   * Match using string-based tags.
   */
  String[] strings() default {};
}