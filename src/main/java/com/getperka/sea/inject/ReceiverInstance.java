package com.getperka.sea.inject;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * A binding annotation for a {@link Object} in an {@link DecoratorScope} to provide the instance
 * that the receiving method will be invoked upon. If the receiver method is static, the value
 * {@link StaticInvocation#INSTANCE} will be injected, since injecting {@code null} value is poor
 * form.
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface ReceiverInstance {
  /*
   * The field is in an inner static class because placing non-constant final fields in an
   * annotation type triggers javac bug 6857918.
   */
  public static class StaticInvocation {
    /**
     * Indicates that the receiver method is static.
     */
    public static final Object INSTANCE = new Object();
  }
}
