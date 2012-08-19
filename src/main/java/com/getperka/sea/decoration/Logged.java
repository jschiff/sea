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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * Triggers a log message (via {@code SLF4J}) whenever the affect event dispatch target is invoked.
 * By default, a simple debug-level message will be written. User-provided messages may be supplied
 * via the optional properties of this annotation.
 */
@EventDecoratorBinding(LoggingDecorator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Logged {
  /**
   * If set, an error message to be logged whenever the event receiver is invoked.
   */
  String error() default "";

  /**
   * If set, an info message to be logged whenever the event receiver is invoked.
   */
  String info() default "";

  /**
   * If set, a warning message to be logged whenever the event receiver is invoked.
   */
  String warn() default "";
}