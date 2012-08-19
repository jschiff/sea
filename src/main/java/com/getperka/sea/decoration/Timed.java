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
import java.util.concurrent.TimeUnit;

import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * Interrupts or stops a receiver thread if it does not complete within the specified time
 * allotment.
 * <p>
 * This example shows how an {@link Object#wait()} can be short-circuited after 100 milliseconds.
 * 
 * <pre>
 * &#064;Timed(100)
 * public class RipVanReceiver {
 *   &#064;Receiver
 *   synchronized void receive(Event e) throws InterruptedException {
 *     wait();
 *   }
 * }
 * </pre>
 */
@EventDecoratorBinding(TimedDecorator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
  /**
   * If {@code false}, the default value, {@link Thread#interrupt()} will be used to attempt to halt
   * the thread. If {@code true}, {@link Thread#stop()} will be used to terminate the thread.
   * <p>
   * Note that the use of {@link Thread#stop()} is inherently unsafe. Additionally, it is possible
   * for the injected exception to be caught by a {@code catch (Throwable t)} block, which may alter
   * the target thread's termination behavior.
   */
  boolean stop() default false;

  /**
   * The unit of measurement for {@link #value()}. The default value is
   * {@link TimeUnit#MILLISECONDS}.
   */
  TimeUnit unit() default TimeUnit.MILLISECONDS;

  /**
   * The duration, expressed as the number of {@link #unit() units} to wait.
   */
  long value();
}
