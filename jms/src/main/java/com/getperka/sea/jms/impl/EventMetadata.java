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

import javax.jms.Destination;

/**
 * Contains additional state about an Event to avoid the need for a base remoteable event type.
 */
public class EventMetadata {

  private Destination replyTo;

  protected EventMetadata() {}

  public Destination getReplyTo() {
    return replyTo;
  }

  public void setReplyTo(Destination replyTo) {
    this.replyTo = replyTo;
  }
}