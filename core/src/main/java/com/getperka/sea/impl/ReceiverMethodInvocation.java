package com.getperka.sea.impl;
/*
 * #%L
 * Simple Event Architecture - Core
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.ReceiverScoped;

/**
 * Encapsulates all of the state necessary to actually invoke the receiver method.
 */
@ReceiverScoped
public class ReceiverMethodInvocation implements Callable<Object> {
  private final Queue<Event> deferredEvents = new ConcurrentLinkedQueue<Event>();
  private Object instance;
  @EventLogger
  @Inject
  private Logger logger;
  private Method method;
  private List<Provider<?>> methodArgumentProviders;
  private final AtomicBoolean wasDispatched = new AtomicBoolean();
  private final AtomicReference<Object> wasReturned = new AtomicReference<Object>();
  private final AtomicReference<Throwable> wasThrown = new AtomicReference<Throwable>();

  /**
   * Requires injection.
   */
  protected ReceiverMethodInvocation() {}

  @Override
  public Object call() throws IllegalArgumentException, IllegalAccessException {
    // Obtain each argument for the method
    Object[] args = new Object[methodArgumentProviders.size()];
    for (int i = 0, j = args.length; i < j; i++) {
      try {
        args[i] = methodArgumentProviders.get(i).get();
      } catch (RuntimeException e) {
        throw new RuntimeException("Could not obtain argument " + i, e);
      }
    }

    // Now dispatch
    try {
      Object value = method.invoke(instance, args);
      wasReturned.set(value);
      return value;
    } catch (InvocationTargetException e) {
      // Clean up the stack trace
      Throwable cause = e.getCause();
      wasThrown.set(cause);
      // Log this error at a reduced level
      logger.debug("Exception added to Decorator.Context", e);
      return null;
    } finally {
      wasDispatched.set(true);
    }
  }

  public void configure(Method method, Object instance, List<Provider<?>> methodArgumentProviders) {
    this.method = method;
    this.instance = instance;
    this.methodArgumentProviders = methodArgumentProviders;
  }

  public Queue<Event> getDeferredEvents() {
    return deferredEvents;
  }

  public Object getReceiverInstance() {
    return instance;
  }

  public boolean getWasDispatched() {
    return wasDispatched.get();
  }

  public Object getWasReturned() {
    return wasReturned.get();
  }

  public Throwable getWasThrown() {
    return wasThrown.get();
  }
}