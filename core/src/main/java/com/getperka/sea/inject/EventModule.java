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
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.impl.DispatchImpl;
import com.getperka.sea.impl.DispatchResultImpl;
import com.getperka.sea.impl.ReceiverTargetImpl;
import com.getperka.sea.impl.SettableReceiverTarget;
import com.getperka.sea.impl.SettableRegistration;
import com.getperka.sea.impl.SettableRegistrationImpl;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class EventModule extends AbstractModule {

  private static class MyFactory implements ThreadFactory {
    private final ThreadGroup g = new ThreadGroup("SEA Dispatch");

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(g, r);
    }
  }

  @Override
  protected void configure() {
    bindReceiverScope();
    bindDecoratorScope();

    bind(DispatchResult.class).to(DispatchResultImpl.class);

    bind(EventDispatch.class).to(DispatchImpl.class);

    // Choose a reasonable default for the thread pool
    bind(ExecutorService.class)
        .annotatedWith(EventExecutor.class)
        .toInstance(executorService());

    bind(Logger.class)
        .annotatedWith(EventLogger.class)
        .toInstance(LoggerFactory.getLogger(EventDispatch.class));

    bind(SettableReceiverTarget.class).to(ReceiverTargetImpl.class);
    bind(SettableRegistration.class).to(SettableRegistrationImpl.class);
  }

  /**
   * Create or return an {@link ExecutorService} for executing events on.
   */
  protected ExecutorService executorService() {
    return Executors.newCachedThreadPool(new MyFactory());
  }

  private void bindDecoratorScope() {
    DecoratorScope decoratorScope = new DecoratorScope();
    bindScope(DecoratorScoped.class, decoratorScope);
    bind(DecoratorScope.class).toInstance(decoratorScope);

    bind(Annotation.class)
        .toProvider(decoratorScope.<Annotation> provider())
        .in(decoratorScope);

    bind(new TypeLiteral<Callable<Object>>() {})
        .toProvider(decoratorScope.<Callable<Object>> provider())
        .in(decoratorScope);
  }

  private void bindReceiverScope() {
    ReceiverScope scope = new ReceiverScope();
    bindScope(ReceiverScoped.class, scope);
    bind(ReceiverScope.class).toInstance(scope);

    bind(Event.class)
        .annotatedWith(CurrentEvent.class)
        .toProvider(scope.<Event> provider())
        .in(scope);
    bind(new TypeLiteral<Queue<Event>>() {})
        .annotatedWith(DeferredEvents.class)
        .toProvider(scope.<Queue<Event>> provider())
        .in(scope);
    bind(Object.class)
        .annotatedWith(EventContext.class)
        .toProvider(scope.<Object> provider())
        .in(scope);
    bind(AtomicBoolean.class)
        .annotatedWith(WasDispatched.class)
        .toProvider(scope.<AtomicBoolean> provider())
        .in(scope);
    bind(new TypeLiteral<AtomicReference<Object>>() {})
        .annotatedWith(WasReturned.class)
        .toProvider(scope.<AtomicReference<Object>> provider())
        .in(scope);
    bind(new TypeLiteral<AtomicReference<Throwable>>() {})
        .annotatedWith(WasThrown.class)
        .toProvider(scope.<AtomicReference<Throwable>> provider())
        .in(scope);
    bind(Object.class)
        .annotatedWith(ReceiverInstance.class)
        .toProvider(scope.<Object> provider())
        .in(scope);
    bind(ReceiverTarget.class)
        .toProvider(scope.<ReceiverTarget> provider())
        .in(scope);
  }
}
