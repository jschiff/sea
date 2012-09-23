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
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.impl.DecoratorContext;
import com.getperka.sea.impl.DispatchImpl;
import com.getperka.sea.impl.DispatchMap;
import com.getperka.sea.impl.Invocation;
import com.getperka.sea.impl.ReceiverTargetImpl;
import com.getperka.sea.impl.SettableReceiverTarget;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;

public class EventModule extends PrivateModule {

  private static class MyFactory implements ThreadFactory {
    private final ThreadGroup g = new ThreadGroup("SEA Dispatch");

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(g, r);
    }
  }

  private final int numThreads = Integer.getInteger("EventModule.numThreads", 128);

  @Override
  protected void configure() {
    EventScope eventScope = new EventScope();
    bindScope(EventScoped.class, eventScope);
    bind(EventScope.class).toInstance(eventScope);

    bindReceiverScope();

    bind(new TypeLiteral<EventDecorator.Context<Annotation, Event>>() {})
        .to(DecoratorContext.class);

    bind(DispatchMap.class).asEagerSingleton();

    bind(Event.class)
        .annotatedWith(CurrentEvent.class)
        .toProvider(eventScope.<Event> provider())
        .in(eventScope);
    expose(Event.class).annotatedWith(CurrentEvent.class);

    bind(EventDispatch.class).to(DispatchImpl.class);
    expose(EventDispatch.class);

    // Create a reasonably-sized pool
    bind(ExecutorService.class).toInstance(
        Executors.newFixedThreadPool(numThreads, new MyFactory()));

    bind(new TypeLiteral<Collection<AnnotatedElement>>() {})
        .annotatedWith(GlobalDecorators.class)
        .toInstance(new ConcurrentLinkedQueue<AnnotatedElement>());

    bind(Invocation.class);

    bind(new TypeLiteral<List<Invocation>>() {}).toProvider(InvocationProvider.class);

    bind(Logger.class)
        .annotatedWith(EventLogger.class)
        .toInstance(LoggerFactory.getLogger(EventDispatch.class));

    bind(SettableReceiverTarget.class).to(ReceiverTargetImpl.class);
  }

  private void bindReceiverScope() {
    DecoratorScope decoratorScope = new DecoratorScope();
    bindScope(DecoratorScoped.class, decoratorScope);
    bind(DecoratorScope.class).toInstance(decoratorScope);

    bind(Annotation.class)
        .toProvider(decoratorScope.<Annotation> provider())
        .in(decoratorScope);

    bind(AtomicBoolean.class)
        .annotatedWith(WasDispatched.class)
        .toProvider(decoratorScope.<AtomicBoolean> provider())
        .in(decoratorScope);

    bind(new TypeLiteral<AtomicReference<Throwable>>() {})
        .annotatedWith(WasThrown.class)
        .toProvider(decoratorScope.<AtomicReference<Throwable>> provider())
        .in(decoratorScope);

    bind(Event.class)
        .toProvider(decoratorScope.<Event> provider())
        .in(decoratorScope);

    bind(ReceiverTarget.class)
        .toProvider(decoratorScope.<ReceiverTarget> provider())
        .in(decoratorScope);

    bind(new TypeLiteral<Callable<Object>>() {})
        .toProvider(decoratorScope.<Callable<Object>> provider())
        .in(decoratorScope);
  }
}
