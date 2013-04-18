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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.EventSubscriberException;
import com.getperka.sea.jms.RoutingMode;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.Subscriptions;
import com.getperka.sea.jms.ext.SubscriptionSource;
import com.getperka.sea.jms.inject.EventConnection;
import com.getperka.sea.jms.inject.EventSession;
import com.google.inject.Injector;

/**
 * Intercepts events from the local EventDispatch and forwards them to the JMS subscription logic.
 */
@Singleton
public class SubscriptionObserver implements EventObserver<Subscriptions, Event> {

  @EventConnection
  @Inject
  Connection connection;
  @Inject
  Injector injector;
  @Inject
  @EventLogger
  Logger logger;
  @Inject
  @EventSession
  Session session;
  @Inject
  EventSubscriber subscriber;

  private ConcurrentMap<Class<? extends Event>, Boolean> shouldSuppress =
      new ConcurrentHashMap<Class<? extends Event>, Boolean>();

  protected SubscriptionObserver() {}

  @Override
  public void initialize(Subscriptions subscriptions) {
    final Map<Class<? extends Event>, SubscriptionOptions> events =
        new HashMap<Class<? extends Event>, SubscriptionOptions>();

    // A simple context implementation that drops subscriptions into the map
    SubscriptionSource.Context ctx = new SubscriptionSource.Context() {
      @Override
      public void subscribe(Class<? extends Event> eventType, SubscriptionOptions options) {
        events.put(eventType, options);
      }
    };

    // Set up declarative subscriptions
    new DeclarativeSubscriptionSource(subscriptions).configureSubscriptions(ctx);

    // Invoke each SubscriptionSource
    for (Class<? extends SubscriptionSource> clazz : subscriptions.sources()) {
      injector.getInstance(clazz).configureSubscriptions(ctx);
    }

    // Perform actual registration
    for (Map.Entry<Class<? extends Event>, SubscriptionOptions> entry : events.entrySet()) {
      Class<? extends Event> eventType = entry.getKey();
      SubscriptionOptions options = entry.getValue();
      try {
        shouldSuppress.put(eventType, RoutingMode.REMOTE.equals(options.routingMode()));
        subscriber.subscribe(eventType, options);
      } catch (EventSubscriberException e) {
        throw new RuntimeException("Could not subscribe to event", e);
      }
    }
  }

  @Override
  public void observeEvent(Context<Event> context) {
    // Allow events being sent by a subscription to be dispatched normally
    if (context.getContext().getUserObject() instanceof EventSubscription) {
      return;
    }
    // Don't send already-suppressed events
    if (context.isSuppressed()) {
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
    try {
      session.close();
    } catch (JMSException e) {
      logger.error("Could not close JMS session", e);
    }
  }

  public void start() throws JMSException {
    connection.start();
  }

  public void stop() throws JMSException {
    connection.stop();
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
