package com.getperka.sea.impl;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.Registration;
import com.getperka.sea.ext.ReceiverTarget;
import com.google.inject.util.Providers;

/**
 * Provides routing of {@link Event} objects to {@link ReceiverTarget} instances.
 */
@Singleton
public class DispatchMap {
  /**
   * Memoizes return values in {@link #getTargets(Class)}.
   */
  private final Map<Class<? extends Event>, List<ReceiverTarget>> cache =
      new ConcurrentHashMap<Class<? extends Event>, List<ReceiverTarget>>();
  /**
   * The main registration datastructure.
   */
  private final Queue<SettableRegistration> registered = new ConcurrentLinkedQueue<SettableRegistration>();
  private Provider<SettableRegistration> registrations;

  protected DispatchMap() {}

  public void cancel(SettableRegistration registration) {
    registered.remove(registration);
    cache.clear();
  }

  /**
   * Returns an immutable list of the {@link ReceiverTarget} instances that should be used when
   * routing a specific type of event.
   */
  public List<ReceiverTarget> getTargets(Class<? extends Event> event) {
    List<ReceiverTarget> toReturn = cache.get(event);
    if (toReturn != null) {
      return toReturn;
    }
    toReturn = findTargets(event);
    cache.put(event, toReturn);
    return toReturn;
  }

  public <T> Registration register(Class<T> receiver, Provider<? extends T> provider) {
    SettableRegistration registration = registrations.get();
    registration.set(receiver, provider);
    registered.add(registration);
    cache.clear();
    return registration;
  }

  public Registration register(Object receiver) {
    @SuppressWarnings("unchecked")
    Class<Object> clazz = (Class<Object>) receiver.getClass();
    return register(clazz, Providers.of(receiver));
  }

  /**
   * Computes the list of {@link ReceiverTarget} instances that should receive the given event type.
   * This method looks for receivers whose event type is assignable from the given event.
   */
  List<ReceiverTarget> findTargets(Class<? extends Event> event) {
    // Use a Set to de-duplicate targets if there are multiple registrations
    Set<ReceiverTarget> toReturn = new HashSet<ReceiverTarget>();

    for (Iterator<SettableRegistration> it = registered.iterator(); it.hasNext();) {
      SettableRegistration registration = it.next();
      toReturn.addAll(registration.getReceiverTargets(event));
    }

    return toReturn.isEmpty() ? Collections.<ReceiverTarget> emptyList() :
        Collections.unmodifiableList(new ArrayList<ReceiverTarget>(toReturn));
  }

  @Inject
  void inject(Provider<SettableRegistration> registrations) {
    this.registrations = registrations;
  }
}
