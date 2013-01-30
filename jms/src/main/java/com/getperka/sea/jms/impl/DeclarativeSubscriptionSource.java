package com.getperka.sea.jms.impl;

/*
 * #%L
 * Simple Event Architecture - JMS Support
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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

import com.getperka.sea.Event;
import com.getperka.sea.jms.Subscription;
import com.getperka.sea.jms.Subscriptions;
import com.getperka.sea.jms.ext.SubscriptionSource;

/**
 * Adapts the declarative {@link Subscriptions} annotation to {@link SubscriptionSource}.
 */
class DeclarativeSubscriptionSource implements SubscriptionSource {

  private final Subscriptions subscriptions;

  public DeclarativeSubscriptionSource(Subscriptions subscriptions) {
    this.subscriptions = subscriptions;
  }

  @Override
  public void configureSubscriptions(Context context) {
    for (Subscription subscription : subscriptions.value()) {
      for (Class<? extends Event> clazz : subscription.event()) {
        context.subscribe(clazz, subscription.options());
      }
    }
  }

}
