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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.getperka.sea.Event;
import com.getperka.sea.ext.ReceiverTarget;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class DecoratorScope extends BaseScope {

  private final ThreadLocal<Annotation> annotation = new ThreadLocal<Annotation>();
  private final ThreadLocal<Event> event = new ThreadLocal<Event>();
  private final ThreadLocal<ReceiverTarget> target = new ThreadLocal<ReceiverTarget>();
  private final ThreadLocal<AtomicBoolean> wasDispatched = new ThreadLocal<AtomicBoolean>();
  private final ThreadLocal<AtomicReference<Throwable>> wasThrown = new ThreadLocal<AtomicReference<Throwable>>();
  private final ThreadLocal<Callable<Object>> work = new ThreadLocal<Callable<Object>>();
  private final Map<Key<?>, ThreadLocal<?>> map = new HashMap<Key<?>, ThreadLocal<?>>();

  DecoratorScope() {
    map.put(Key.get(Annotation.class), annotation);
    map.put(Key.get(AtomicBoolean.class, WasDispatched.class), wasDispatched);
    map.put(Key.get(new TypeLiteral<AtomicReference<Throwable>>() {}, WasThrown.class), wasThrown);
    map.put(Key.get(Event.class), event);
    map.put(Key.get(ReceiverTarget.class), target);
    map.put(Key.get(new TypeLiteral<Callable<Object>>() {}), work);
  }

  public void exit() {
    for (ThreadLocal<?> local : map.values()) {
      local.remove();
    }
  }

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    ThreadLocal<?> local = map.get(key);
    if (local != null) {
      return cast(new ThreadLocalProvider<Object>(local));
    }
    return unscoped;
  }

  public DecoratorScope withAnnotation(Annotation annotation) {
    this.annotation.set(annotation);
    return this;
  }

  public DecoratorScope withEvent(Event event) {
    this.event.set(event);
    return this;
  }

  public DecoratorScope withTarget(ReceiverTarget target) {
    this.target.set(target);
    return this;
  }

  public DecoratorScope withWasDispatched(AtomicBoolean wasDispatched) {
    this.wasDispatched.set(wasDispatched);
    return this;
  }

  public DecoratorScope withWasThrown(AtomicReference<Throwable> wasThrown) {
    this.wasThrown.set(wasThrown);
    return this;
  }

  public DecoratorScope withWork(Callable<Object> work) {
    this.work.set(work);
    return this;
  }
}
