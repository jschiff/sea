package com.getperka.sea.jms;
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

/**
 * Defines a set of behaviors for how events should be routed.
 */
public enum EventProfile {
  /**
   * Announcement events are sent to each node participating in the application stack. If re-fired
   * after being received, the event will be sent to all nodes again.
   */
  ANNOUNCEMENT,
  /**
   * An event that is received by every node in the application, but when re-fired returns to the
   * node that originally sent the event.
   */
  SCATTER_GATHER,
  /**
   * A request-response style of event. When fired, the event will be received by exactly one node.
   * If re-fired after being received, the event will only be received by the original sender.
   */
  WORK;
}
