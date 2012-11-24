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

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;

/**
 * The EventSubscriber controls how events are routed across a JMS domain. Specific event types must
 * be registered with the EventSubscriber in order to send or receive them. Additionally, the event
 * type must be supported by the {@link EventTransport} in use.
 * 
 * @see EventSubscribers#create
 */
public interface EventSubscriber {
  /**
   * Terminate all subscriptions.
   */
  void shutdown();

  /**
   * Subscribe to an event. If the event type (or its superclass) declares a
   * {@link SubscriptionOptions} annotation, it will be used. If no annotation is specified, the
   * annotation defaults will be used. The default modes essentially extend the behavior of a local
   * {@link EventDispatch} instance across an arbitrary number of subscribers.
   */
  void subscribe(Class<? extends Event> eventType) throws EventSubscriberException;

  /**
   * Subscribe to an event with specific options.
   */
  void subscribe(Class<? extends Event> eventType, SubscriptionOptions options)
      throws EventSubscriberException;
}
