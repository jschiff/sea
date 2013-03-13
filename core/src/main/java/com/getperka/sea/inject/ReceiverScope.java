package com.getperka.sea.inject;

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.ReceiverTarget;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;

/**
 * Instances of ReceiverScope are expected to be accessed only from within a single thread. They are
 * reentrant, to support synchronous event dispatch if necessary.
 */
public class ReceiverScope extends BaseScope {

  private static class Frame {
    final Map<Key<?>, Object> values = new ConcurrentHashMap<Key<?>, Object>();

    Frame(Event event, ReceiverTarget receiverTarget, EventContext context) {
      values.put(currentEventKey, event);
      values.put(eventContextKey, context);
      values.put(receiverTargetKey, receiverTarget);
    }
  }

  private static final Key<Event> currentEventKey = Key.get(Event.class, CurrentEvent.class);
  private static final Key<EventContext> eventContextKey = Key.get(EventContext.class);
  private static final Key<ReceiverTarget> receiverTargetKey = Key.get(ReceiverTarget.class);

  private final ThreadLocal<Deque<Frame>> frameStack = new ThreadLocal<Deque<Frame>>() {
    @Override
    protected Deque<Frame> initialValue() {
      return new ArrayDeque<ReceiverScope.Frame>();
    }
  };

  public void enter(Event event, ReceiverTarget receiverTarget, EventContext context) {
    Frame frame = new Frame(event, receiverTarget, context);
    frameStack.get().push(frame);
  }

  public void exit() {
    frameStack.get().pop();
  }

  public boolean inReceiver() {
    return !frameStack.get().isEmpty();
  }

  @Override
  public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
    return new MapProvider<T>(key, unscoped) {
      @Override
      protected Map<Key<?>, Object> scopeMap() {
        Frame frame = frameStack.get().peek();
        if (frame == null) {
          throw new OutOfScopeException("Not in a ReceiverScope");
        }
        return frame.values;
      }
    };
  }
}
