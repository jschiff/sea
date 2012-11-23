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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;

@Singleton
class OrderedDispatchImpl implements OrderedDispatch {
  /**
   * Used for propagating the "next" event after an ordered event is successfully dispatched.
   */
  private final EventDispatch dispatch;
  /**
   * Maintains the state for all ordered events to pass through the dispatch.
   */
  private final Map<Event, BatchState> state =
      Collections.synchronizedMap(new WeakHashMap<Event, BatchState>());

  @Inject
  OrderedDispatchImpl(EventDispatch dispatch) {
    this.dispatch = dispatch;
  }

  @Override
  public void fire(Collection<? extends Event> events) {
    // Create a new ordered collection
    BatchState batchState = new BatchState(events);
    Event first = null;
    for (Event e : events) {
      if (first == null) {
        first = e;
      }
      state.put(e, batchState);
      // Route all of the events to the EventDispatch, since there may be non-ordered receivers
      dispatch.fire(e);
    }
  }

  BatchState getState(Event e) {
    return state.get(e);
  }
}
