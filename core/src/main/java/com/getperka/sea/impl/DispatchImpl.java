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

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Registration;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.SuspendedEvent;
import com.getperka.sea.inject.EventExecutor;
import com.google.inject.Injector;

@Singleton
public class DispatchImpl implements EventDispatch, HasInjector {

  @Inject
  private BindingMap bindingMap;
  private final AtomicLong count = new AtomicLong();
  @Inject
  private Provider<ReceiverMethodInvocation> currentInvocation;
  @Inject
  private DecoratorMap decoratorMap;
  @Inject
  private Injector injector;
  @Inject
  private InvocationManager invocationManager;
  @Inject
  private ObserverMap observers;
  @Inject
  private DispatchMap map;
  @EventExecutor
  @Inject
  private ExecutorService service;
  private AtomicBoolean shutdown = new AtomicBoolean();

  protected DispatchImpl() {}

  @Override
  public void addGlobalDecorator(AnnotatedElement element) {
    bindingMap.register(element);
    decoratorMap.register(element);
    observers.register(element);
  }

  @Override
  public void fire(Event event) {
    fire(event, null);
  }

  @Override
  public void fire(Event event, final Object userObject) {
    if (shutdown.get() || event == null) {
      return;
    }
    EventContext context = new EventContext() {
      final long sequenceNumber = count.incrementAndGet();

      @Override
      public long getSequenceNumber() {
        return sequenceNumber;
      }

      @Override
      public Object getUserObject() {
        return userObject;
      }

      @Override
      public SuspendedEvent suspend() {
        return currentInvocation.get().suspend();
      }
    };
    if (!observers.shouldFire(event, context)) {
      return;
    }
    List<ReceiverStackInvocation> allInvocation = invocationManager.getInvocations(event, context);
    for (ReceiverStackInvocation invocation : allInvocation) {
      if (invocation.isSynchronous()) {
        // Invocation.call() shouldn't generally throw exceptions unless things are very broken
        invocation.call();
      } else {
        service.submit(invocation);
      }
    }
  }

  @Override
  public Injector getInjector() {
    return injector;
  }

  @Override
  public Registration register(Class<?> receiver) {
    return register(receiver, null);
  }

  @Override
  public <T> Registration register(Class<T> receiver, Provider<? extends T> provider) {
    return map.register(receiver, provider);
  }

  @Override
  public Registration register(Object receiver) {
    return map.register(receiver);
  }

  @Override
  public Registration registerWeakly(Object receiver) {
    return map.registerWeakly(receiver);
  }

  @Override
  public void setDraining(boolean drain) {
    invocationManager.setDraining(drain);
  }

  @Override
  public void shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      setDraining(true);
      observers.shutdown();
    }
  }
}
