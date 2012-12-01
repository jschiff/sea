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

import com.getperka.sea.EventDispatch;

/**
 * Controls the destination of an event when it is passed to {@link EventDispatch#fire} after having
 * been received.
 */
public enum ReturnMode {
  /**
   * Return the event to the JMS reply-to Destination.
   */
  RETURN_TO_SENDER,
  /**
   * Use the default {@link SubscriptionOptions#sendMode()} for the event.
   */
  USE_SEND_MODE;
}
