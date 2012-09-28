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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.inject.DecoratorScope;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.EventScope;
import com.getperka.sea.inject.GlobalDecorators;
import com.getperka.sea.inject.ReceiverInstance;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Encapsulates a method and an instance on which to execute it. Information about
 * {@link EventDecorator} configuration is memoized in instances of this class to reduce reflection
 * costs.
 */
public class ReceiverTargetImpl implements SettableReceiverTarget {
  private class Work implements Callable<Object> {
    // These two variable are temporary state for the actual dispatch
    private final AtomicBoolean wasDispatched = new AtomicBoolean();
    private final AtomicReference<Throwable> wasThrown = new AtomicReference<Throwable>();
    private final Event event;
    private final Object instance;

    public Work(DecoratorScope decoratorScope, Event event) {
      this.event = event;
      instance = instanceProvider == null ? null : instanceProvider.get();
      decoratorScope
          .withWasDispatched(wasDispatched)
          .withReceiverInstance(instance == null ? ReceiverInstance.STATIC_INVOCATION : instance)
          .withWasThrown(wasThrown);
    }

    @Override
    public Object call() throws IllegalArgumentException, IllegalAccessException {
      try {
        return method.invoke(instance, event);
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
  }

  /**
   * The configuration annotations to be passed to the instances returned by the
   * {@link #decoratorProviders} Providers.
   */
  private final List<Annotation> decoratorAnnotations = new ArrayList<Annotation>();
  /**
   * Vends instances of {@link EventDecorator.Context}.
   */
  private Provider<EventDecorator.Context<Annotation, Event>> decoratorContexts;
  /**
   * Providers for the {@link EventDecorator} types that should be applied when dispatching an event
   * to the receiver method.
   */
  private final List<Provider<EventDecorator<Annotation, Event>>> decoratorProviders =
      new ArrayList<Provider<EventDecorator<Annotation, Event>>>();
  /**
   * Scope data for constructing {@link EventDecorator} instances.
   */
  private DecoratorScope decoratorScope;
  /**
   * Scope data for constructing various components.
   */
  private EventScope eventScope;
  /**
   * Injected configuration for top-level decorators.
   */
  private Collection<AnnotatedElement> globalDecorators;
  /**
   * Used to retrieve references to providers.
   */
  private Injector injector;
  /**
   * Set via {@link #setInstanceDispatch} or {@link #setStaticDispatch}.
   */
  private Provider<?> instanceProvider;
  /**
   * Mainly reports errors from {@link Work}.
   */
  private Logger logger;
  /**
   * Set via {@link #setInstanceDispatch} or {@link #setStaticDispatch}.
   */
  private Method method;

  protected ReceiverTargetImpl() {}

  @Override
  public Object dispatch(final Event event) throws Exception {
    eventScope.enter(event);
    decoratorScope
        .withEvent(event)
        .withTarget(this);
    try {

      Callable<Object> toInvoke = new Work(decoratorScope, event);

      Iterator<Annotation> aIt = decoratorAnnotations.iterator();
      Iterator<Provider<EventDecorator<Annotation, Event>>> edIt = decoratorProviders.iterator();
      while (aIt.hasNext() && edIt.hasNext() && toInvoke != null) {
        Annotation annotation = aIt.next();
        decoratorScope.withAnnotation(annotation).withWork(toInvoke);

        EventDecorator<Annotation, Event> eventDecorator = edIt.next().get();
        /*
         * If the decorator can't receive the event, just drop it. This allows decorators that are
         * specific to a certain event subtype to be applied to a receiver method that accepts a
         * wider event type.
         */
        ParameterizedType type = (ParameterizedType) TypeLiteral.get(eventDecorator.getClass())
            .getSupertype(EventDecorator.class)
            .getType();
        Type[] typeArgs = type.getActualTypeArguments();
        if (TypeLiteral.get(typeArgs[0]).getRawType().isAssignableFrom(annotation.annotationType())
          && TypeLiteral.get(typeArgs[1]).getRawType().isAssignableFrom(event.getClass())) {
          toInvoke = eventDecorator.wrap(decoratorContexts.get());
        }
      }

      return toInvoke == null ? null : toInvoke.call();
    } finally {
      decoratorScope.exit();
      eventScope.exit();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReceiverTargetImpl)) {
      return false;
    }
    ReceiverTargetImpl other = (ReceiverTargetImpl) o;
    // Object identity comparison intentional
    return (instanceProvider == other.instanceProvider
      || instanceProvider.equals(other.instanceProvider)) &&
      (method == other.method || method.equals(other.method));
  }

  @Override
  public int hashCode() {
    return (instanceProvider == null ? 0 : instanceProvider.hashCode()) * 13 +
      (method == null ? 0 : method.hashCode()) * 7;
  }

  @Override
  public void setInstanceDispatch(Provider<?> provider, Method method) {
    this.instanceProvider = provider;
    this.method = method;
    computeDecorators();
  }

  @Override
  public void setStaticDispatch(Method staticMethod) {
    if (!Modifier.isStatic(staticMethod.getModifiers())) {
      throw new IllegalArgumentException();
    }
    instanceProvider = null;
    method = staticMethod;
    computeDecorators();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    // Add annotations
    for (Annotation a : decoratorAnnotations) {
      sb.append(a).append("\n");
    }

    // void com.example.Foo.bar(com.example.Event)
    sb.append(method.getReturnType().getName()).append(" ")
        .append(method.getDeclaringClass().getName()).append(".")
        .append(method.getName()).append("(");
    boolean needsComma = false;
    for (Class<?> clazz : method.getParameterTypes()) {
      if (needsComma) {
        sb.append(", ");
      } else {
        needsComma = true;
      }
      sb.append(clazz.getName());
    }
    sb.append(")");
    return sb.toString();
  }

  @Inject
  void inject(
      Provider<EventDecorator.Context<Annotation, Event>> decoratorContexts,
      DecoratorScope decoratorScope,
      EventScope eventScope,
      @GlobalDecorators Collection<AnnotatedElement> globalDecorators,
      Injector injector,
      @EventLogger Logger logger) {
    this.decoratorContexts = decoratorContexts;
    this.decoratorScope = decoratorScope;
    this.eventScope = eventScope;
    this.globalDecorators = globalDecorators;
    this.injector = injector;
    this.logger = logger;
  }

  private void computeDecorators() {
    decoratorAnnotations.clear();
    decoratorProviders.clear();
    computeDecorators(method);
    computeDecorators(method.getDeclaringClass());
    computeDecorators(method.getDeclaringClass().getPackage());
    for (AnnotatedElement global : globalDecorators) {
      computeDecorators(global);
    }
    assert decoratorAnnotations.size() == decoratorProviders.size();
  }

  private void computeDecorators(AnnotatedElement elt) {
    for (final Annotation a : elt.getDeclaredAnnotations()) {
      EventDecoratorBinding binding = a.annotationType().getAnnotation(EventDecoratorBinding.class);
      if (binding != null) {
        Class<? extends EventDecorator<?, ?>> clazz = binding.value();
        // Get the provider and add it to the accumulators
        @SuppressWarnings("unchecked")
        Provider<EventDecorator<Annotation, Event>> provider =
            (Provider<EventDecorator<Annotation, Event>>) injector.getProvider(clazz);
        decoratorAnnotations.add(a);
        decoratorProviders.add(provider);
      }
    }
  }
}
