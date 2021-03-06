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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.Receiver;
import com.getperka.sea.Registration;
import com.getperka.sea.ext.ConfigurationProvider;
import com.getperka.sea.ext.ConfigurationVisitor;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.EventLogger;
import com.google.inject.Injector;

public class RegistrationImpl implements ConfigurationProvider, Registration {
  private DispatchMap dispatchMap;
  private Provider<ReceiverTargetImpl> dispatchTargets;
  private Injector injector;
  private Logger logger;
  private Map<Class<? extends Event>, List<ReceiverTarget>> targets = Collections.emptyMap();

  protected RegistrationImpl() {}

  @Override
  public void accept(ConfigurationVisitor visitor) {
    for (List<ReceiverTarget> list : targets.values()) {
      for (ReceiverTarget target : list) {
        ((ConfigurationProvider) target).accept(visitor);
      }
    }
  }

  @Override
  public void cancel() {
    targets = Collections.emptyMap();
    dispatchMap.cancel(this);
  }

  public List<ReceiverTarget> getReceiverTargets(Class<? extends Event> event) {
    List<ReceiverTarget> toReturn = new ArrayList<ReceiverTarget>();
    for (Map.Entry<Class<? extends Event>, List<ReceiverTarget>> entry : targets.entrySet()) {
      if (entry.getKey().isAssignableFrom(event)) {
        toReturn.addAll(entry.getValue());
      }
    }

    return toReturn;
  }

  public boolean isCanceled() {
    return targets.isEmpty();
  }

  public <T> void set(Class<T> receiver, Provider<? extends T> provider) {
    // Accumulate state in a new map
    Map<Class<? extends Event>, List<ReceiverTarget>> temp =
        new HashMap<Class<? extends Event>, List<ReceiverTarget>>();

    Class<? super T> lookAt = receiver;
    while (lookAt != null) {
      for (Method m : lookAt.getDeclaredMethods()) {
        Receiver annotation = m.getAnnotation(Receiver.class);
        // Ignore anything not explicitly annotated to receive events
        if (annotation == null) {
          continue;
        }

        // Create a ReceiverTarget to handle dispatch to the method
        ReceiverTargetImpl target = dispatchTargets.get();
        target.setSynchronous(annotation.synchronous());
        if (Modifier.isStatic(m.getModifiers())) {
          target.setStaticDispatch(m);
        } else {
          if (provider == null) {
            provider = injector.getProvider(receiver);
          }
          target.setInstanceDispatch(provider, m);
        }

        Class<? extends Event> event = target.getEventType();
        if (event == null) {
          logger.warn("Ignoring {}.{} because it does not receive an Event type",
              receiver.getName(), m.getName());
          continue;
        }

        List<ReceiverTarget> list = temp.get(event);
        if (list == null) {
          list = new ArrayList<ReceiverTarget>();
          temp.put(event, list);
        }

        // Create a ReceiverTarget that will dispatch to the method
        list.add(target);
        logger.debug("{}.{} will receive {}",
            new Object[] { receiver.getName(), m.getName(), event.getName() });
      }
      lookAt = lookAt.getSuperclass();
    }

    // Make the datastructure immutable
    for (Map.Entry<?, List<ReceiverTarget>> entry : temp.entrySet()) {
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }
    targets = Collections.unmodifiableMap(temp);
  }

  @Override
  public String toString() {
    return targets.toString();
  }

  @Inject
  void inject(DispatchMap dispatchMap, Provider<ReceiverTargetImpl> dispatchTargets,
      Injector injector, @EventLogger Logger logger) {
    this.dispatchMap = dispatchMap;
    this.dispatchTargets = dispatchTargets;
    this.injector = injector;
    this.logger = logger;
  }
}
