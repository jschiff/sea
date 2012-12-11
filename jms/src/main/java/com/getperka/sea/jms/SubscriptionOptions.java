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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

import com.getperka.sea.jms.decorator.SuppressLocalEvents;

/**
 * Controls options related to Event routing.
 * 
 * @see SubscriptionOptionsBuilder
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubscriptionOptions {
  // NB: When adding additional methods, update the builder

  /**
   * Allows the name of the queue or topic to be overridden. If left unspecified, the canonical name
   * of the event type will be used.
   */
  String destinationName() default "";

  /**
   * Durable subscriptions are used with {@link SendMode#TOPIC} to ensure that all messages sent to
   * a topic will eventually be received by a subscriber, even if the process is temporarily halted.
   */
  String durableSubscriberId() default "";

  /**
   * A JMS message selector expression used to filter messages received by a subscription.
   * 
   * @see Message
   */
  String messageSelector() default "";

  /**
   * By default, any JMS messages that are published by an EventSubscriber will not be received by
   * that subscriber. This prevents an "echo effect" where an event is received locally via direct
   * event dispatch and again from the subscriber receiving the JMS message it just sent. This is
   * only a concern if a local receiver exists for remote event types.
   * <p>
   * By setting this property to {@code false} and using a {@link SuppressLocalEvents} filter, all
   * direct dispatches can be disabled, instead requiring local event receivers to be driven by the
   * EventSubscriber from messages in the JMS destination.
   */
  boolean preventEchoEffect() default true;

  /**
   * An event's return mode determines how the event is routed if it is re-fired after being
   * received from a JMS destination. The default is to use the {@link #sendMode()}.
   */
  ReturnMode returnMode() default ReturnMode.USE_SEND_MODE;

  /**
   * An event can be sent either via a JMS {@link Queue} or a {@link Topic}. The former is provides
   * single-issue distribution semantics, while the latter provides broadcast semantics. The default
   * is {@link SendMode#TOPIC}.
   */
  SendMode sendMode() default SendMode.TOPIC;

  /**
   * A subscriber can elect not to send or receive certain events by using an alternate
   * {@link SubscriptionMode}. The default behavior is to send, receive, and acknowledge events.
   */
  SubscriptionMode subscriptionMode() default SubscriptionMode.DEFAULT;
}
