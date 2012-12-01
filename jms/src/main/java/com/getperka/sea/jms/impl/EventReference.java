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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.getperka.sea.Event;

/**
 * A stand-off for an Event that maintain object identity.
 */
public class EventReference {
  private final WeakReference<Event> evt;
  private final int hashCode;

  public EventReference(Event evt, ReferenceQueue<? super Event> queue) {
    if (evt == null) {
      throw new IllegalArgumentException("Null event");
    }
    this.evt = new WeakReference<Event>(evt, queue);
    this.hashCode = System.identityHashCode(evt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EventReference)) {
      return false;
    }
    EventReference other = (EventReference) o;
    Event myEvent = evt.get();
    return myEvent != null && myEvent == other.evt.get();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}