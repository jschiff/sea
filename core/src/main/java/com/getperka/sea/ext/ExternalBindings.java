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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.getperka.sea.EventDispatch;

/**
 * Allows arbitrary annotations to be bound to {@link EventDecorator} or {@link EventObserver}
 * types. This is useful when the annotation is provided by an external library or cannot be
 * annotated with either {@link EventDecoratorBinding} or {@link EventObserverBinding}. External
 * bindings are registered via {@link EventDispatch#addGlobalDecorator}.
 * <p>
 * For example, the JSR-250 annotation {@code RolesAllowed} could be used to impose security
 * constraints on receivers:
 * 
 * <pre>
 * &#064;ExternalBindings({
 *     &#064;ExternalBinding(annotation = RolesAllowed.class, decorator = RolesAllowedDecorator.class)
 * })
 * class Bootstrap {
 *   void setUpEvents() {
 *     eventDispatch.addGlobalDecorator(Bootstrap.class);
 *   }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface ExternalBindings {
  ExternalBinding[] value();
}
