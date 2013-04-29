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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.JMSException;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.Subscriptions;
import com.getperka.sea.jms.ext.SubscriptionSource;
import com.getperka.sea.jms.inject.EventConnection;
import com.google.inject.Injector;

/**
 * Intercepts events from the local EventDispatch and forwards them to the JMS subscription logic.
 */
@Singleton
public class SubscriptionObserver implements EventObserver<Subscriptions, Event> {

  /**
   * Utility method to indicate if the event dispatch was triggered from a JMS queue.
   * <p>
   * TODO: This belongs somewhere else, along with the start() and stop() methods.
   * 
   * @param userObject the user object returned from {@link EventContext#getUserObject()}.
   */
  public static boolean isFromJms(Object userObject) {
    return userObject instanceof MessageBridge;
  }

  @EventConnection
  @Inject
  Connection connection;
  @Inject
  Injector injector;
  @Inject
  @EventLogger
  Logger logger;
  @Inject
  MessageBridge messageThread;

  protected SubscriptionObserver() {}

  public void drain() {
    try {
      messageThread.drain();
    } catch (Exception e) {
      logger.error("Could not shut down MessageThread", e);
    }
  }

  @Override
  public void initialize(final Subscriptions subscriptions) {
    final Map<Class<? extends Event>, SubscriptionOptions> events =
        new HashMap<Class<? extends Event>, SubscriptionOptions>();

    // A simple context implementation that drops subscriptions into the map
    SubscriptionSource.Context ctx = new SubscriptionSource.Context() {
      @Override
      public void subscribe(Class<? extends Event> eventType, SubscriptionOptions options) {
        events.put(eventType, options);
      }

      @Override
      public Subscriptions subscriptions() {
        return subscriptions;
      }
    };

    // Set up declarative subscriptions
    new DeclarativeSubscriptionSource(subscriptions).configureSubscriptions(ctx);

    // Invoke each SubscriptionSource
    for (Class<? extends SubscriptionSource> clazz : subscriptions.sources()) {
      injector.getInstance(clazz).configureSubscriptions(ctx);
    }

    if (!messageThread.isAlive()) {
      messageThread.start();
      messageThread.setApplicationName(subscriptions.applicationName());
    }

    // Perform actual registration
    for (Map.Entry<Class<? extends Event>, SubscriptionOptions> entry : events.entrySet()) {
      Class<? extends Event> eventType = entry.getKey();
      SubscriptionOptions options = entry.getValue();
      try {
        messageThread.subscribe(eventType, options);
      } catch (Exception e) {
        throw new RuntimeException("Could not subscribe to event", e);
      }
    }
  }

  @Override
  public void observeEvent(Context<Event> context) {
    // Allow events being sent by a subscription to be dispatched normally
    if (SubscriptionObserver.isFromJms(context.getContext().getUserObject())) {
      return;
    }
    // Don't send already-suppressed events
    if (context.isSuppressed()) {
      return;
    }
    try {
      messageThread.maybeSendToJms(context.getOriginalEvent(), context.getContext());
    } catch (Exception e) {
      logger.error("Could not send event to JMS", e);
    }
  }

  @Override
  public void shutdown() {
    try {
      connection.close();
    } catch (JMSException e) {
      logger.error("Could not close Connection", e);
    }
  }

  public void start() throws JMSException {
    connection.start();
  }
}
