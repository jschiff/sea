package com.getperka.sea.jms.impl;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.jms.EventSubscriberException;
import com.getperka.sea.jms.RoutingMode;
import com.getperka.sea.jms.Subscription;
import com.getperka.sea.jms.Subscriptions;

@Singleton
public class SubscriptionObserver implements EventObserver<Subscriptions, Event> {

  private ConcurrentMap<Class<? extends Event>, Boolean> shouldSuppress =
      new ConcurrentHashMap<Class<? extends Event>, Boolean>();
  private EventSubscriber subscriber;

  protected SubscriptionObserver() {}

  @Override
  public void initialize(Subscriptions subscriptions) {
    for (Subscription subscription : subscriptions.value()) {
      try {
        shouldSuppress.put(subscription.event(),
            RoutingMode.REMOTE.equals(subscription.options().routingMode()));
        subscriber.subscribe(subscription.event(), subscription.options());
      } catch (EventSubscriberException e) {
        throw new RuntimeException("Could not subscribe to event", e);
      }
    }
  }

  @Override
  public void observeEvent(Context<Event> context) {
    // Allow events being sent by a subscription to be dispatched normally
    if (context.getContext() instanceof EventSubscription) {
      return;
    }
    if (shouldSuppress(context.getEvent().getClass())) {
      context.suppressEvent();
    }
    subscriber.fire(context.getOriginalEvent(), context.getContext());
  }

  @Override
  public void shutdown() {
    subscriber.shutdown();
  }

  @Inject
  void inject(EventSubscriber subscriber) {
    this.subscriber = subscriber;
  }

  private boolean shouldSuppress(Class<? extends Event> eventType) {
    Boolean suppress = shouldSuppress.get(eventType);
    if (Boolean.TRUE.equals(suppress)) {
      return true;
    }

    // Shouldn't be the usual case, but look for the first assignable type and copy that value
    for (Map.Entry<Class<? extends Event>, Boolean> entry : shouldSuppress.entrySet()) {
      if (entry.getKey().isAssignableFrom(eventType)) {
        suppress = entry.getValue();
        shouldSuppress.put(eventType, suppress);
        return suppress;
      }
    }

    // Memoize ignoring everything else
    shouldSuppress.put(eventType, false);
    return false;
  }
}
