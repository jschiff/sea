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

    bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool(new ThreadFactory() {
      private final ThreadGroup g = new ThreadGroup("SEA Dispatch");

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(g, r);
      }
    }));

    bind(new TypeLiteral<Collection<AnnotatedElement>>() {})
        .annotatedWith(GlobalDecorators.class)
        .toInstance(new ConcurrentLinkedQueue<AnnotatedElement>());

    bind(Invocation.class);

    bind(new TypeLiteral<List<Invocation>>() {}).toProvider(InvocationProvider.class);

    bind(Logger.class).toInstance(LoggerFactory.getLogger(EventDispatch.class));

    bind(SettableReceiverTarget.class).to(ReceiverTargetImpl.class);
  }

  private void bindReceiverScope() {
    DecoratorScope receiverScope = new DecoratorScope();
    bindScope(DecoratorScoped.class, receiverScope);
    bind(DecoratorScope.class).toInstance(receiverScope);

    bind(Annotation.class)
        .toProvider(receiverScope.<Annotation> provider())
        .in(receiverScope);

    bind(Event.class)
        .toProvider(receiverScope.<Event> provider())
        .in(receiverScope);

    bind(ReceiverTarget.class)
        .toProvider(receiverScope.<ReceiverTarget> provider())
        .in(receiverScope);

    bind(new TypeLiteral<Callable<Object>>() {})
        .toProvider(receiverScope.<Callable<Object>> provider())
        .in(receiverScope);
  }
}
