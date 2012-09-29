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

import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * The receiver responsible for updating the {@link OutcomeEvent} with the result of the
 * computation.
 * 
 * @see OutcomeEvent
 */
@Documented
@EventDecoratorBinding(ImplementationFilter.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface Implementation {
  /**
   * By default, the {@link Implementation} decorator will re-fire the {@link OutcomeEvent} to the
   * current {@link EventDispatch} once the receiver method exits. Setting this property to
   * {@code false} will disable this behavior.
   */
  boolean fireResult() default true;
}