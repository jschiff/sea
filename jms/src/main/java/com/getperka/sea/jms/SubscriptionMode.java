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
 * Controls whether or not a subscriber chooses to send, receive, or acknowledge events.
 */
public enum SubscriptionMode {
  /**
   * Send, receive, and acknowledge events.
   */
  DEFAULT(true, true),
  /**
   * Send, but do not receive events.
   */
  SEND(false, true),
  /**
   * Receive and acknowledge events.
   */
  RECEIVE(true, false),
  /**
   * Receive, but not not acknowledge, events. The actual effect of this mode will depend on how
   * your JMS implementation handles messages that are left unacknowledged.
   */
  SPY(true, false) {
    @Override
    public boolean shouldAcknowledge() {
      return false;
    }
  };

  private final boolean receive;
  private final boolean send;

  private SubscriptionMode(boolean receive, boolean send) {
    this.receive = receive;
    this.send = send;
  }

  public boolean shouldAcknowledge() {
    return true;
  }

  public boolean shouldReceive() {
    return receive;
  }

  public boolean shouldSend() {
    return send;
  }
}
