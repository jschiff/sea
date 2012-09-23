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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.DecoratorScope;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.EventScope;
import com.getperka.sea.inject.EventScoped;
import com.getperka.sea.inject.GlobalDecorators;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

@EventScoped
public class Invocation implements Callable<Object> {
  private Provider<EventDecorator.Context<Annotation, Event>> decoratorContexts;
  private DecoratorScope decoratorScope;
  private Event event;
  private EventScope eventScope;
  private Collection<AnnotatedElement> globalDecorators;
  private Injector injector;
  private Logger logger;
  private Callable<Object> toInvoke;
  private ReceiverTarget target;

  protected Invocation() {}

  @Override
  public Object call() throws Exception {
    Thread.currentThread().setName(toString());
    eventScope.enter(event);
    try {
      final AtomicBoolean wasDispatched = new AtomicBoolean();
      final AtomicReference<Throwable> wasThrown = new AtomicReference<Throwable>();
      toInvoke = new Callable<Object>() {
        @Override
        public Object call() throws IllegalArgumentException, IllegalAccessException {
          try {
            return target.getMethod().invoke(target.getInstance(), event);
          } catch (InvocationTargetException e) {
            // Provide stack trace in a useful context
            wasThrown.set(e.getCause());
            return null;
          } finally {
            wasDispatched.set(true);
          }
        }
      };

      // Set up the decorator scope to return information
      decoratorScope
          .withEvent(event)
          .withTarget(target)
          .withWasDispatched(wasDispatched)
          .withWasThrown(wasThrown);

      Method method = target.getMethod();
      instantiateDecorators(method);
      instantiateDecorators(method.getDeclaringClass());
      instantiateDecorators(method.getDeclaringClass().getPackage());
      for (AnnotatedElement global : globalDecorators) {
        instantiateDecorators(global);
      }
      return toInvoke == null ? null : toInvoke.call();
    } catch (Exception e) {
      logger.error("Unhandled exception during event dispatch", e);
      throw e;
    } finally {
      decoratorScope.exit();
      eventScope.exit();
      Thread.currentThread().setName("idle");
    }
  }

  public void setInvocation(final ReceiverTarget target) {
    this.target = target;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return target.toString();
  }

  @Inject
  void inject(
      Provider<EventDecorator.Context<Annotation, Event>> decoratorContexts,
      DecoratorScope decoratorScope,
      @CurrentEvent Event event,
      EventScope eventScope,
      @GlobalDecorators Collection<AnnotatedElement> globalDecorators,
      Injector injector,
      @EventLogger Logger logger) {
    this.decoratorContexts = decoratorContexts;
    this.decoratorScope = decoratorScope;
    this.event = event;
    this.eventScope = eventScope;
    this.globalDecorators = globalDecorators;
    this.injector = injector;
    this.logger = logger;
  }

  private void instantiateDecorators(AnnotatedElement elt) {
    for (final Annotation a : elt.getDeclaredAnnotations()) {
      if (toInvoke == null) {
        return;
      }
      EventDecoratorBinding binding = a.annotationType().getAnnotation(EventDecoratorBinding.class);
      if (binding != null) {
        Class<? extends EventDecorator<?, ?>> clazz = binding.value();
        @SuppressWarnings("unchecked")
        EventDecorator<Annotation, Event> decorator =
            (EventDecorator<Annotation, Event>) injector.getInstance(clazz);
        /*
         * If the decorator can't receive the event, just drop it. This allows decorators that are
         * specific to a certain event subtype to be applied to a receiver method that accepts a
         * wider event type.
         */
        ParameterizedType type = (ParameterizedType) TypeLiteral.get(clazz)
            .getSupertype(EventDecorator.class)
            .getType();
        Type[] typeArgs = type.getActualTypeArguments();
        if (!TypeLiteral.get(typeArgs[0]).getRawType().isAssignableFrom(a.annotationType())
          || !TypeLiteral.get(typeArgs[1]).getRawType().isAssignableFrom(event.getClass())) {
          continue;
        }
        decoratorScope.withAnnotation(a).withWork(toInvoke);
        toInvoke = decorator.wrap(decoratorContexts.get());
      }
    }
  }
}
