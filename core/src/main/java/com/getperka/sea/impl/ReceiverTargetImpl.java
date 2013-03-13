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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

import org.slf4j.Logger;

import com.getperka.sea.BadReceiverException;
import com.getperka.sea.BaseCompositeEvent;
import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.impl.DecoratorMap.DecoratorInfo;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.DecoratorScope;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.ReceiverScope;
import com.google.inject.BindingAnnotation;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Encapsulates a method and an instance on which to execute it. Information about
 * {@link EventDecorator} configuration is memoized in instances of this class to reduce reflection
 * costs.
 */
public class ReceiverTargetImpl implements ReceiverTarget {
  /**
   * Vends instances of {@link EventDecorator.Context}.
   */
  private Provider<DecoratorContext> decoratorContexts;
  /**
   * Holds information about annotation bindings.
   */
  private DecoratorMap decoratorMap;
  /**
   * Scope data for constructing {@link EventDecorator} instances.
   */
  private DecoratorScope decoratorScope;
  private EventDispatch dispatch;
  /**
   * Scope data for constructing various components.
   */
  private ReceiverScope receiverScope;
  /**
   * The type of event that the ReceiverTarget expects to receive.
   */
  private Class<? extends Event> eventType;
  /**
   * Used to retrieve references to providers.
   */
  private Injector injector;
  /**
   * Set via {@link #setInstanceDispatch} or {@link #setStaticDispatch}.
   */
  private Provider<?> instanceProvider;
  /**
   * Mainly reports errors from {@link ReceiverMethodInvocation}.
   */
  private Logger logger;
  /**
   * Set via {@link #setInstanceDispatch} or {@link #setStaticDispatch}.
   */
  private Method method;
  /**
   * Contains providers for each argument of the method, including the current event.
   */
  private List<Provider<?>> methodArgumentProviders;
  private Provider<DispatchResult> results;
  private Provider<ReceiverMethodInvocation> works;

  protected ReceiverTargetImpl() {}

  @Override
  public DispatchResult dispatch(Event event, EventContext context) {
    if (event == null || context == null) {
      throw new IllegalArgumentException();
    }
    receiverScope.enter(event, this, context);
    try {
      ReceiverMethodInvocation work = works.get();
      Object instance = instanceProvider == null ? null : instanceProvider.get();

      // If this is an instance target without an instance, don't do any work
      if (instanceProvider != null && instance == null) {
        return results.get();
      }

      work.configure(method, instance, methodArgumentProviders);

      Callable<Object> toInvoke = work;
      for (DecoratorInfo info : decoratorMap.getDecoratorInfo(method)) {
        Annotation annotation = info.getAnnotation();
        decoratorScope.enter(annotation, toInvoke);
        try {
          EventDecorator<Annotation, Event> eventDecorator = info.getProvider().get();
          /*
           * If the decorator can't receive the event, just drop it. This allows decorators that are
           * specific to a certain event subtype to be applied to a receiver method that accepts a
           * wider event type.
           */
          ParameterizedType type = (ParameterizedType) TypeLiteral.get(eventDecorator.getClass())
              .getSupertype(EventDecorator.class)
              .getType();
          Type[] typeArgs = type.getActualTypeArguments();

          // Does the decorator accept the annotation that we're currently looking at?
          if (TypeLiteral.get(typeArgs[0]).getRawType()
              .isAssignableFrom(annotation.annotationType())) {
            // Determine the Event type that the decorator wants
            Class<? extends Event> expectedEventType =
                TypeLiteral.get(typeArgs[1]).getRawType().asSubclass(Event.class);
            // Cast or extract the desired event facet
            Event desiredFacet = BaseCompositeEvent.asEventFacet(expectedEventType, event);
            if (desiredFacet == null) {
              continue;
            }
            // Create the context, set the facet, and wrap
            DecoratorContext ctx = decoratorContexts.get();
            ctx.setEvent(desiredFacet);
            toInvoke = eventDecorator.wrap(ctx);

            // If the decorator has nullified the work, don't do anything else
            if (toInvoke == null) {
              break;
            }
          }
        } finally {
          decoratorScope.exit();
        }
      }

      if (toInvoke != null) {
        try {
          toInvoke.call();
          for (Event deferred : work.getDeferredEvents()) {
            dispatch.fire(deferred);
          }
        } catch (Exception e) {
          logger.error("Unhandled exception while dispatching event", e);
        }
      }

      return results.get();
    } finally {
      decoratorScope.exit();
      receiverScope.exit();
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

  public Class<? extends Event> getEventType() {
    return eventType;
  }

  @Override
  public int hashCode() {
    return (instanceProvider == null ? 0 : instanceProvider.hashCode()) * 13 +
      (method == null ? 0 : method.hashCode()) * 7;
  }

  public void setInstanceDispatch(Provider<?> provider, Method method) {
    this.instanceProvider = provider;
    this.method = method;
    method.setAccessible(true);
    computeProviders();
  }

  public void setStaticDispatch(Method staticMethod) {
    if (!Modifier.isStatic(staticMethod.getModifiers())) {
      throw new IllegalArgumentException();
    }
    instanceProvider = null;
    method = staticMethod;
    method.setAccessible(true);
    computeProviders();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

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
      Provider<DecoratorContext> decoratorContexts,
      DecoratorMap decoratorMap,
      DecoratorScope decoratorScope,
      EventDispatch dispatch,
      ReceiverScope eventScope,
      Injector injector,
      @EventLogger Logger logger,
      Provider<DispatchResult> results,
      Provider<ReceiverMethodInvocation> works) {
    this.decoratorContexts = decoratorContexts;
    this.decoratorMap = decoratorMap;
    this.decoratorScope = decoratorScope;
    this.dispatch = dispatch;
    this.receiverScope = eventScope;
    this.injector = injector;
    this.logger = logger;
    this.results = results;
    this.works = works;
  }

  /**
   * Compute the providers for the arguments of the method to invoke.
   */
  private void computeProviders() {
    Key<Event> keyCurrentEvent = Key.get(Event.class, CurrentEvent.class);
    Annotation[][] annotations = method.getParameterAnnotations();
    Type[] params = method.getGenericParameterTypes();
    methodArgumentProviders = new ArrayList<Provider<?>>(params.length);

    for (int i = 0, j = params.length; i < j; i++) {
      Type param = params[i];
      Class<?> rawParamType = TypeLiteral.get(param).getRawType();
      Annotation binding = null;

      // First, see if there's a binding annotation
      for (Annotation a : annotations[i]) {
        if (a.annotationType().isAnnotationPresent(BindingAnnotation.class)
          || a.annotationType().isAnnotationPresent(Qualifier.class)) {
          binding = a;
          break;
        }
      }

      Key<?> key;
      if (binding == null) {
        if (Event.class.isAssignableFrom(rawParamType)) {
          // An un-annotated reference to an event type will be considered a CurrentEvent reference
          key = keyCurrentEvent;
        } else {
          // Otherwise, just ask for an unannotated binding
          key = Key.get(param);
        }
      } else {
        // Annotated type binding
        key = Key.get(param, binding);
      }

      if (CurrentEvent.class.equals(key.getAnnotationType())) {
        eventType = rawParamType.asSubclass(Event.class);
      }

      try {
        methodArgumentProviders.add(injector.getProvider(key));
      } catch (ConfigurationException e) {
        throw new BadReceiverException("Cannot compute injection binding for parameter " + i,
            this, e);
      }
    }
  }
}
