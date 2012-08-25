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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.Receiver;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.EventLogger;
import com.google.inject.Injector;
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
  private Injector injector;
  private Logger logger;
  /**
   * The main registration datastructure. Maps {@link Event} types onto {@link ReceiverTarget}
   * instances.
   */
  private final Map<Class<? extends Event>, Queue<ReceiverTarget>> map =
      new ConcurrentHashMap<Class<? extends Event>, Queue<ReceiverTarget>>();
  /**
   * Prevents duplicate registrations of receiver types.
   */
  private final Set<Class<?>> registered = new HashSet<Class<?>>();

  private Provider<SettableReceiverTarget> dispatchTargets;

  @Inject
  protected DispatchMap() {}

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

  public synchronized <T> void register(Class<T> receiver, Provider<? extends T> provider) {
    if (!registered.add(receiver)) {
      return;
    }

    for (Method m : receiver.getDeclaredMethods()) {
      if (!m.isAnnotationPresent(Receiver.class)) {
        continue;
      }
      Class<?>[] params = m.getParameterTypes();
      if (params.length != 1) {
        logger.warn("Ignoring {}.{} becasue it has more than one argument",
            receiver.getName(), m.getName());
        continue;
      }
      if (!Event.class.isAssignableFrom(params[0])) {
        logger.warn("Ignoring {}.{} because its sole argument is not assignable to Event",
            receiver.getName(), m.getName());
        continue;
      }
      Class<? extends Event> event = params[0].asSubclass(Event.class);

      Queue<ReceiverTarget> queue = map.get(event);
      if (queue == null) {
        queue = new ConcurrentLinkedQueue<ReceiverTarget>();
        // Only mutated in this synchronized method, so don't need putIfAbsent()
        map.put(event, queue);
      }

      m.setAccessible(true);
      SettableReceiverTarget target = dispatchTargets.get();
      if (Modifier.isStatic(m.getModifiers())) {
        target.setStaticDispatch(m);
      } else {
        if (provider == null) {
          provider = injector.getProvider(receiver);
        }
        target.setInstanceDispatch(provider, m);
      }
      queue.add(target);
      logger.debug("{}.{} will receive {}",
          new Object[] { receiver.getName(), m.getName(), event.getName() });
    }
    cache.clear();
  }

  public void register(Object receiver) {
    @SuppressWarnings("unchecked")
    Class<Object> clazz = (Class<Object>) receiver.getClass();
    register(clazz, Providers.of(receiver));
  }

  /**
   * Computes the list of {@link ReceiverTarget} instances that should receive the given event type.
   * This method looks for receivers whose event type is assignable from the given event.
   */
  List<ReceiverTarget> findTargets(Class<? extends Event> event) {
    List<ReceiverTarget> toReturn = new ArrayList<ReceiverTarget>();
    for (Map.Entry<Class<? extends Event>, Queue<ReceiverTarget>> entry : map.entrySet()) {
      if (entry.getKey().isAssignableFrom(event)) {
        toReturn.addAll(entry.getValue());
      }
    }
    return toReturn.isEmpty() ? Collections.<ReceiverTarget> emptyList() :
        Collections.unmodifiableList(toReturn);
  }

  @Inject
  void inject(Provider<SettableReceiverTarget> dispatchTargets, Injector injector,
      @EventLogger Logger logger) {
    this.dispatchTargets = dispatchTargets;
    this.logger = logger;
    this.injector = injector;
  }
}
