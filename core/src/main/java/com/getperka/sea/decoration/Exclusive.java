package com.getperka.sea.decoration;
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * Prevent concurrent dispatch of a particular {@link Event} instance to all receivers with this
 * filter.
 * <p>
 * Applying this decoration does not prevent a receiver from being called concurrently with other
 * event instances. Preventing concurrent invocations of a receiver method can be achieved by using
 * the {@code synchronized} keyword on the method.
 * <p>
 * The scope of the locking is always the {@link EventDispatch} instance, regardless of the element
 * on which the annotation is placed.
 */
@Documented
@EventDecoratorBinding(ExclusiveFilter.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface Exclusive {}
