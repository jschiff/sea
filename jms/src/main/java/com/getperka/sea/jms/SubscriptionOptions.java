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

import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Controls options related to Event routing.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubscriptionOptions {

  /**
   * Controls the destination type used when routing an event message.
   */
  public enum DestinationType {
    /**
     * Use a JMS {@link Queue}. Events sent via a queue will be received by exactly one subscriber.
     */
    QUEUE,
    /**
     * Use a JMS {@link Topic}. Events sent via a topic will be received by all subscribers.
     */
    TOPIC;
  }

  /**
   * Allows the name of the queue or topic to be overridden. If left unspecified, the canonical name
   * of the event type will be used.
   */
  String destinationName() default "";

  /**
   * Durable subscriptions are used with {@link DestinationType#TOPIC} to ensure that all messages
   * sent to a topic will eventually be received by a subscriber, even if the process is temporarily
   * halted.
   */
  String durableSubscriberId() default "";

  /**
   * An event's return mode determines how the event is routed if it is re-fired after being
   * received. The default mode is to send the event via a topic so that all subscribers may see the
   * event. Electing to return it via a queue allows the re-fired event to be sent back only to the
   * subscriber that originally sent it. The latter is appropriate when the event is the result of a
   * stateful process and is meaningless or uninteresting to an arbitrary subscriber.
   */
  DestinationType returnMode() default DestinationType.TOPIC;

  /**
   * An event can be sent either via a JMS {@link Queue} or a {@link Topic}. The former is provides
   * single-issue distribution semantics, while the latter provides broadcast semantics.
   */
  DestinationType sendMode() default DestinationType.TOPIC;
}
