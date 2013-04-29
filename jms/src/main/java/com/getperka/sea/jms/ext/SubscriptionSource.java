package com.getperka.sea.jms.ext;

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
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.Subscriptions;

/**
 * A hook point for programmatic event subscription configuration.
 * 
 * @see Subscriptions#sources()
 */
public interface SubscriptionSource {

  /**
   * Provides access to event subscription configuration.
   * <p>
   * This interface is subject to expansion.
   */
  public interface Context {
    /**
     * Subscribe the given event types.
     * <p>
     * If the same event type is registered more than once, a "last one wins" policy will be used.
     * Subscriptions created through this method have priority over declarative subscriptions.
     * 
     * @param eventType the type to create a subscription for
     * @param options the options that will govern the subscriptions.
     * @see SubscriptionOptions#DEFAULT
     * @see SubscriptionOptionsBuilder
     */
    void subscribe(Class<? extends Event> eventType, SubscriptionOptions options);

    /**
     * Returns the top-level Subscriptions annotation.
     */
    Subscriptions subscriptions();
  }

  /**
   * Called during subscription configuration when additional subscriptions are to be added.
   * 
   * @param context A configuration context which will be provided by the plumbing
   */
  void configureSubscriptions(Context context);
}
