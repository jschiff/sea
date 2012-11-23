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

import com.getperka.sea.Event;
import com.google.inject.Key;
import com.google.inject.Provider;

public class EventScope extends BaseScope {
  private final ThreadLocal<Deque<Event>> event = new ThreadLocal<Deque<Event>>() {
    @Override
    protected Deque<Event> initialValue() {
      return new ArrayDeque<Event>();
    }
  };

  public void enter(Event event) {
    this.event.get().push(event);
  }

  public void exit() {
    this.event.get().pop();
  }

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    if (CurrentEvent.class.equals(key.getAnnotationType())) {
      return cast(new Provider<Event>() {
        @Override
        public Event get() {
          return event.get().peek();
        }
      });
    }
    return unscoped;
  }
}
