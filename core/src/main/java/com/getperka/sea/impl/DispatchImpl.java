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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Registration;
import com.getperka.sea.inject.EventExecutor;
import com.getperka.sea.inject.EventScope;
import com.getperka.sea.inject.GlobalDecorators;
import com.google.inject.Injector;

@Singleton
public class DispatchImpl implements EventDispatch, HasInjector {

  private Collection<AnnotatedElement> globalDecorators;
  private Injector injector;
  private Provider<List<Invocation>> invocations;
  private DispatchMap map;
  private EventScope scope;
  private ExecutorService service;
  private volatile boolean shutdown;

  protected DispatchImpl() {}

  @Override
  public void addGlobalDecorator(AnnotatedElement element) {
    globalDecorators.add(element);
  }

  @Override
  public void fire(Event event) {
    if (shutdown || event == null) {
      return;
    }
    scope.enter(event);
    try {
      for (Invocation invocation : invocations.get()) {
        service.submit(invocation);
      }
    } finally {
      scope.exit();
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
  public void shutdown() {
    shutdown = true;
    service.shutdown();
  }

  @Inject
  void inject(@GlobalDecorators Collection<AnnotatedElement> globalDecorators,
      Injector injector, Provider<List<Invocation>> invocations, EventScope scope,
      DispatchMap map, @EventExecutor ExecutorService service) {
    this.globalDecorators = globalDecorators;
    this.injector = injector;
    this.invocations = invocations;
    this.map = map;
    this.scope = scope;
    this.service = service;
  }
}