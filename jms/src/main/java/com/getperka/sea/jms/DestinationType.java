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

import javax.jms.Queue;
import javax.jms.Topic;

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