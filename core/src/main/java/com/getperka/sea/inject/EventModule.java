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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.ext.SuspendedEvent;
import com.getperka.sea.impl.DispatchImpl;
import com.getperka.sea.impl.DispatchResultImpl;
import com.getperka.sea.impl.ReceiverStackInvocation;
import com.getperka.sea.impl.SuspendedEventImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

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
    bind(SuspendedEvent.class).to(SuspendedEventImpl.class);
  }

  /**
   * Create or return an {@link ExecutorService} for executing events on.
   */
  @Provides
  @EventExecutor
  @Singleton
  protected ExecutorService executorService() {
    return Executors.newCachedThreadPool(new MyFactory());
  }

  @Provides
  @EventLogger
  @Singleton
  protected Logger logger(@EventLogger ILoggerFactory factory) {
    return factory.getLogger(EventDispatch.class.getName());
  }

  @Provides
  @EventLogger
  @Singleton
  protected ILoggerFactory loggerFactory() {
    return LoggerFactory.getILoggerFactory();
  }

  private void bindDecoratorScope() {
    DecoratorScope decoratorScope = new DecoratorScope();
    bindScope(DecoratorScoped.class, decoratorScope);
    bind(DecoratorScope.class).toInstance(decoratorScope);
  }

  private void bindReceiverScope() {
    ReceiverScope scope = new ReceiverScope();
    bindScope(ReceiverScoped.class, scope);
    bind(ReceiverScope.class).toInstance(scope);

    bind(Event.class)
        .annotatedWith(CurrentEvent.class)
        .toProvider(scope.<Event> provider())
        .in(scope);
    bind(EventContext.class)
        .toProvider(scope.<EventContext> provider())
        .in(scope);
    bind(ReceiverStackInvocation.class)
        .toProvider(scope.<ReceiverStackInvocation> provider())
        .in(scope);
    bind(ReceiverTarget.class)
        .toProvider(scope.<ReceiverTarget> provider())
        .in(scope);
  }
}
