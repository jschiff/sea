package com.getperka.sea.order;

/*
 * #%L
 * Simple Event Architecture
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

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.getperka.sea.Event;
import com.getperka.sea.ext.ReceiverTarget;

/**
 * Tracks the remaining Events that should be dispatched to a particular {@link ReceiverTarget}.
 */
class ReceiverState {
  /**
   * Stores the Event that is next in line to be dispatched to the ReceiverTarget.
   */
  private final AtomicReference<Event> next = new AtomicReference<Event>();
  /**
   * The remaining Events, exclusive of {@link #next}, to be dispatched to the
   * {@link ReceiverTarget}.
   */
  private final Queue<Event> toDispatch;

  public ReceiverState(Collection<Event> toDispatch) {
    this.toDispatch = new ConcurrentLinkedQueue<Event>(toDispatch);
    getNext();
  }

  /**
   * Returns the Event that should be fired next.
   */
  public Event getNext() {
    Event toReturn = toDispatch.poll();
    if (!next.compareAndSet(null, toReturn)) {
      throw new IllegalStateException("Calling getNext() without having staged the previous event");
    }
    return toReturn;
  }

  /**
   * Returns {@code true} if the given Event should be the next Event dispatched to the
   * ReceiverTarget. This method will only return {@code true} once for any given Event, unless
   * {@link #pushBack(Event)} is called.
   */
  public boolean isNext(Event event) {
    return next.compareAndSet(event, null);
  }

  /**
   * Resets the currently-pending {@link Event}. This method must only be called after a successful
   * call to {@link #isNext(Event)}.
   */
  public void pushBack(Event event) {
    if (!next.compareAndSet(null, event)) {
      throw new IllegalStateException("Attempting to push back when an Event was already pending");
    }
  }
}
