package com.getperka.sea.impl;

/*
 * #%L
 * Simple Event Architecture - Core
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.BaseCompositeEvent;
import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventObserver;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Responsible for tracking all {@link EventObserver} mappings.
 */
@Singleton
public class ObserverMap {
  static class ObserverRegistration {
    final Annotation annotation;
    final EventObserver<Annotation, Event> observer;

    public ObserverRegistration(Annotation annotation, EventObserver<Annotation, Event> observer) {
      this.annotation = annotation;
      this.observer = observer;
    }
  }

  private BindingMap bindingMap;
  private Injector injector;
  /**
   * The ordered list of active observers. Uses a {@link CopyOnWriteArrayList} to avoid
   * {@link ConcurrentModificationException} since new observers are rarely registered.
   */
  private final List<ObserverRegistration> registrations =
      new CopyOnWriteArrayList<ObserverMap.ObserverRegistration>();

  protected ObserverMap() {}

  /**
   * Each subsequent registration has higher precedence, to match the behavior of
   * {@link EventDispatch#addGlobalDecorator}.
   */
  public void register(AnnotatedElement element) {
    List<Annotation> orderedAnnotation = DecoratorMap.orderedAnnotations(element);

    List<ObserverRegistration> newRegistrations = new ArrayList<ObserverRegistration>();

    for (Annotation annotation : orderedAnnotation) {
      Class<? extends EventObserver<?, ?>> observerType = bindingMap.getObserver(annotation);
      if (observerType == null) {
        continue;
      }

      @SuppressWarnings("unchecked")
      Provider<EventObserver<Annotation, Event>> provider =
          (Provider<EventObserver<Annotation, Event>>) injector.getProvider(observerType);

      EventObserver<Annotation, Event> observer = provider.get();
      observer.initialize(annotation);

      newRegistrations.add(new ObserverRegistration(annotation, observer));
    }

    registrations.addAll(0, newRegistrations);
  }

  public boolean shouldFire(final Event event, final EventContext context) {
    final AtomicBoolean shouldFire = new AtomicBoolean(true);
    for (ObserverRegistration registration : registrations) {
      final Annotation annotation = registration.annotation;
      EventObserver<Annotation, Event> filter = registration.observer;

      // Check provider to annotation assignability, for sanity's sake
      ParameterizedType type = (ParameterizedType) TypeLiteral.get(filter.getClass())
          .getSupertype(EventObserver.class)
          .getType();
      Type[] typeArgs = type.getActualTypeArguments();

      // Determine the Annotation and Event type that the filter wants
      Class<?> desiredAnnotationType = TypeLiteral.get(typeArgs[0]).getRawType();
      Class<? extends Event> desiredEventType =
          TypeLiteral.get(typeArgs[1]).getRawType().asSubclass(Event.class);

      if (desiredAnnotationType.isAssignableFrom(annotation.annotationType())) {
        // Cast or extract the desired event facet
        final Event desiredFacet = BaseCompositeEvent.asEventFacet(desiredEventType, event);
        if (desiredFacet == null) {
          continue;
        }

        EventObserver.Context<Event> ctx = new EventObserver.Context<Event>() {
          @Override
          public EventContext getContext() {
            return context;
          }

          @Override
          public Event getEvent() {
            return desiredFacet;
          }

          @Override
          public Event getOriginalEvent() {
            return event;
          }

          @Override
          public boolean isSuppressed() {
            return !shouldFire.get();
          }

          @Override
          public void suppressEvent() {
            shouldFire.set(false);
          }
        };

        // Allow all filters to fire
        filter.observeEvent(ctx);
      }
    }
    return shouldFire.get();
  }

  public void shutdown() {
    for (ObserverRegistration registration : registrations) {
      registration.observer.shutdown();
    }
    registrations.clear();
  }

  /**
   * Visible for testing.
   */
  List<Annotation> getAnnotations() {
    List<Annotation> toReturn = new ArrayList<Annotation>(registrations.size());
    for (ObserverRegistration registration : registrations) {
      toReturn.add(registration.annotation);
    }
    return toReturn;
  }

  @Inject
  void inject(BindingMap bindingMap, Injector injector) {
    this.bindingMap = bindingMap;
    this.injector = injector;
  }
}
