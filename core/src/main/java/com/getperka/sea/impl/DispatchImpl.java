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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Registration;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.EventExecutor;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.GlobalDecorators;
import com.google.inject.Injector;

@Singleton
public class DispatchImpl implements EventDispatch, HasInjector {

  private Collection<AnnotatedElement> globalDecorators;
  private Injector injector;
  private Provider<Invocation> invocations;
  private Logger logger;
  private DispatchMap map;
  private ExecutorService service;
  private volatile boolean shutdown;

  protected DispatchImpl() {}

  @Override
  public void addGlobalDecorator(AnnotatedElement element) {
    globalDecorators.add(element);
  }

  @Override
  public void fire(Event event) {
    fire(event, null);
  }

  @Override
  public void fire(Event event, Object context) {
    if (shutdown || event == null) {
      return;
    }
    List<Invocation> allInvocation = getInvocations(event, context);
    for (Invocation invocation : allInvocation) {
      service.submit(invocation);
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

  List<Invocation> getInvocations(Event event, Object context) {
    Class<? extends Event> eventClass = event.getClass();
    List<ReceiverTarget> targets = map.getTargets(eventClass);
    List<Invocation> toReturn = new ArrayList<Invocation>();

    // Fire an empty DispatchComplete if there are no receivers
    if (targets.isEmpty() && !(event instanceof DispatchCompleteEvent)) {
      logger.debug("No @Receiver methods that accept {} have been registered",
          eventClass.getName());

      DispatchCompleteEvent complete = new DispatchCompleteEvent();
      complete.setSource(event);
      fire(complete);
      return toReturn;
    }

    Invocation.State state = new Invocation.State(targets.size());

    for (ReceiverTarget target : targets) {
      Invocation invocation = invocations.get();
      invocation.setContext(context);
      invocation.setEvent(event);
      invocation.setInvocation(target);
      invocation.setState(state);
      toReturn.add(invocation);
    }

    return toReturn;
  }

  @Inject
  void inject(@GlobalDecorators Collection<AnnotatedElement> globalDecorators,
      Injector injector, Provider<Invocation> invocations, @EventLogger Logger logger,
      DispatchMap map, @EventExecutor ExecutorService service) {
    this.globalDecorators = globalDecorators;
    this.injector = injector;
    this.invocations = invocations;
    this.logger = logger;
    this.map = map;
    this.service = service;
  }
}
