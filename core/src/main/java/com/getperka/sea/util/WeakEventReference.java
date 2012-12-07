package com.getperka.sea.util;

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

import javax.inject.Provider;

import com.getperka.sea.Event;

/**
 * A weak reference to an Event. This class implements equality behavior based on the underlying
 * identity of the Event. It is suitable for use as a map key even after the underlying event has
 * been garbage-collected.
 */
public class WeakEventReference<E extends Event> extends WeakReference<E> implements Provider<E> {
  private final int hashCode;

  public WeakEventReference(E evt) {
    this(evt, null);
  }

  public WeakEventReference(E evt, ReferenceQueue<? super E> queue) {
    super(evt, queue);
    if (evt == null) {
      throw new IllegalArgumentException("Cannot reference a null event");
    }
    this.hashCode = System.identityHashCode(evt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WeakEventReference)) {
      return false;
    }
    WeakEventReference<?> other = (WeakEventReference<?>) o;
    E myEvent = get();
    return myEvent != null && myEvent == other.get();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}