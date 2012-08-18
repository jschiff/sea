package com.getperka.sea.log;

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
   * If set, an error message to be logged whenever the dispatch target is invoked.
   */
  String error() default "";

  /**
   * If set, an info message to be logged whenever the dispatch target is invoked.
   */
  String info() default "";

  /**
   * If set, a warning message to be logged whenever the dispatch target is invoked.
   */
  String warn() default "";
}