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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies an order in which to apply {@link EventDecorator} and {@link EventObserver} binding
 * annotations. The Nth entry in the array will have an opportunity to act before the N+1st entry.
 * <p>
 * For example, it would be preferable to open a JPA EntityManager after any log handling is set up
 * for a receiver:
 * 
 * <pre>
 * &#064;Logged
 * &#064;OpenJpaEntityManager
 * &#064;DecoratorOrder({ Logged.class, OpenJpaEntityManager.class })
 * public class MyApp {
 *   public static void main(String[] args) {
 *     EventDispatch dispatch = EventDispatcher.create();
 *     dispatch.addGlobalDecorator(MyApp.class, Logged.class, OpenJpaEntityManager.class);
 *     // Do work
 *   }
 * }
 * </pre>
 * 
 * Any binding annotation not referenced in a DecoratorOrder will be applied in an arbitrary order
 * after any referenced binding.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface DecoratorOrder {
  /**
   * The order in which to apply decorator binding annotations.
   */
  Class<? extends Annotation>[] value();
}
