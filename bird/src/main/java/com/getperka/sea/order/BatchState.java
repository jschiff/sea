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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.getperka.sea.Event;
import com.getperka.sea.ext.ReceiverTarget;

/**
 * Tracking data for a batch of ordered events. Instances of BatchState are retained by a
 * weakly-keyed map in {@link OrderedDispatchImpl}.
 */
class BatchState {
  private final ConcurrentMap<ReceiverTarget, ReceiverState> receiverState =
      new ConcurrentHashMap<ReceiverTarget, ReceiverState>();
  /**
   * This ordering must use weak references to ensure that events can be garbage-collected.
   * Otherwise, the WeakHashMap in OrderedDispatchImpl could never discard its keys.
   */
  private final List<WeakReference<Event>> toDispatch;

  public BatchState(Collection<? extends Event> toDispatch) {
    List<WeakReference<Event>> temp = new ArrayList<WeakReference<Event>>(toDispatch.size());
    for (Event event : toDispatch) {
      temp.add(new WeakReference<Event>(event));
    }
    this.toDispatch = Collections.unmodifiableList(temp);
  }

  public ReceiverState getState(ReceiverTarget target) {
    ReceiverState toReturn = receiverState.get(target);
    if (toReturn == null) {
      /*
       * Ensure that none of the original events have been garbage collected. This would indicate
       * that there is some other decorator in play that is discarding events to an @Ordered
       * receiver. Because the loss of any of the original events would prevent this code from
       * honoring its general contract of "these events issued in an exact order", it's better to
       * drop all remaining events rather than performing some kind of partial dispatch.
       */
      List<Event> canStillDispatch = new ArrayList<Event>(toDispatch.size());
      for (WeakReference<Event> ref : toDispatch) {
        Event event = ref.get();
        if (event == null) {
          return new ReceiverState(Collections.<Event> emptyList());
        }
        canStillDispatch.add(event);
      }
      toReturn = new ReceiverState(canStillDispatch);
      ReceiverState previous = receiverState.putIfAbsent(target, toReturn);
      if (previous != null) {
        toReturn = previous;
      }
    }
    return toReturn;
  }
}