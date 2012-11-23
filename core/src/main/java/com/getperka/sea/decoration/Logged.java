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
 * Triggers a log message (via {@code SLF4J}) whenever the decorated event receiver method is
 * invoked or if the receiver method throws an exception. By default, a debug-level messages will be
 * logged.
 * <p>
 * This annotation is especially useful to apply to unit-test receivers that enforce assertions.
 */
@EventDecoratorBinding(LoggingDecorator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Logged {
  /**
   * Maps to SLF4J log levels.
   */
  public enum Level {
    OFF,
    DEBUG,
    INFO,
    WARN,
    ERROR;
  }

  /**
   * Logs any exceptions thrown by the receiver method at this level. Defaults to
   * {@link Level#DEBUG}.
   */
  Level exceptionLevel() default Level.DEBUG;

  /**
   * The log level for the string message. Defaults to {@link Level#DEBUG}.
   */
  Level level() default Level.DEBUG;

  /**
   * An optional message to be logged before the event is dispatched to the receiver.
   */
  String value() default "";
}