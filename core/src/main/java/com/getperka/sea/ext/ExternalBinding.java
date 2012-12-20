package com.getperka.sea.ext;
/*
 * #%L
 * Simple Event Architecture - Core
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.getperka.sea.impl.NoOpTarget;

/**
 * A binding from an arbitrary annotation to an {@link EventDecorator} or {@link EventObserver}.
 * 
 * @see ExternalBindings
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ExternalBinding {
  /**
   * The external annotation to be bound.
   */
  Class<? extends Annotation> annotation();

  /**
   * An {@link EventDecorator} that the annotation should trigger.
   */
  Class<? extends EventDecorator<?, ?>> decorator() default NoOpTarget.class;

  /**
   * An {@link EventObserver} that the annotation should trigger.
   */
  Class<? extends EventObserver<?, ?>> observer() default NoOpTarget.class;
}
