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

import com.getperka.sea.Event;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * Used to filter {@link DispatchCompleteEvent} instances.
 */
@Documented
@EventDecoratorBinding(DispatchedFilter.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface Dispatched {
  /**
   * Filters DispatchCompleteEvent by their source event type.
   */
  Class<? extends Event> eventType() default Event.class;

  /**
   * If {@code true}, filters out events that were not received by any receiver method.
   */
  boolean onlyReceived() default false;

  /**
   * If {@code true}, filters out events that were received by at least one receiver method.
   */
  boolean onlyUnreceived() default false;
}
