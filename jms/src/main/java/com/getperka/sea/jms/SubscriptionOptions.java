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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import javax.jms.Message;

import com.getperka.sea.ext.EventTransport;
import com.getperka.sea.jms.ext.SubscriptionOptionsBuilder;

/**
 * Controls options related to Event routing.
 * 
 * @see SubscriptionOptionsBuilder
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface SubscriptionOptions {
  // NB: When adding additional methods, update the builder

  /**
   * A SubscriptionOptions instance with default values.
   */
  SubscriptionOptions DEFAULT = new SubscriptionOptionsBuilder().build();

  /**
   * The number of event messages that should be processed concurrently for the event type. This
   * control is used to mitigate a "thundering herd" problem where a very large number of events of
   * a particular type are generated in a short time frame.
   * <p>
   * The default value of {@code 0} will disable throttling.
   */
  int concurrencyLevel() default 0;

  /**
   * Override the destination name for the event. If unspecified, the event will be sent to a queue
   * or topic whose name is {@link Subscriptions#applicationName()}.
   */
  String destinationName() default "";

  /**
   * A JMS message selector expression used to filter messages received by a subscription. Custom
   * {@link EventTransport} instances can add additional properties to the messages to allow for
   * selective filtering of subscriptions.
   * 
   * @see Message
   */
  String messageSelector() default "";

  /**
   * The TTL for JMS messages sent by this subscription, measured in {@link #messageTtlUnit()}.
   * <p>
   * The default value {@code 0} means that messages do not have a TTL.
   */
  long messageTtl() default 0;

  /**
   * The unit of measurement for {@link #messageTtl()}.
   * <p>
   * The default value is {@link TimeUnit#SECONDS}.
   */
  TimeUnit messageTtlUnit() default TimeUnit.SECONDS;

  /**
   * Describes a pattern for how events should behave when sent over JMS.
   */
  EventProfile profile() default EventProfile.ANNOUNCEMENT;

  /**
   * A subscriber can elect not to send or receive certain events by using an alternate
   * {@link SubscriptionMode}. The default behavior is to send, receive, and acknowledge events.
   */
  SubscriptionMode subscriptionMode() default SubscriptionMode.DEFAULT;
}
