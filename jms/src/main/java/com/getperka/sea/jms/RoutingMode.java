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

/**
 * Controls how events are routed.
 */
public enum RoutingMode {
  /**
   * Allows events to be routed directly to local receives.
   */
  LOCAL,
  /**
   * Allows events to be routed locally, with the caveat that a duplicate event may also be received
   * via JMS.
   */
  LOCAL_ALLOW_DUPS,
  /**
   * Force events to be routed through JMS so all events are effectively remote.
   */
  REMOTE;
}
