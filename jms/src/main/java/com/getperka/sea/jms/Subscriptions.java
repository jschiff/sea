package com.getperka.sea.jms;

/*
 * #%L
 * Simple Event Architecture - JMS Support
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

import com.getperka.sea.ext.EventObserverBinding;
import com.getperka.sea.jms.ext.SubscriptionSource;
import com.getperka.sea.jms.impl.SubscriptionObserver;

/**
 * A global decorator annotation that enables event types to be routed across JMS.
 */
@EventObserverBinding(SubscriptionObserver.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.METHOD, ElementType.TYPE })
public @interface Subscriptions {
  /**
   * Subscriptions may be configured dynamically by providing SubscriptionSource types. The
   * implementing types must have zero-arg constructors or injection bindings.
   */
  Class<? extends SubscriptionSource>[] sources() default {};

  /**
   * Individual event subscription configurations.
   */
  Subscription[] value() default {};
}
