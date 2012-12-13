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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.BaseCompositeEvent;
import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.ext.EventObserverBinding;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Responsible for tracking all {@link EventObserver} mappings.
 */
@Singleton
public class ObserverMap {
  private final List<Annotation> annotations = new ArrayList<Annotation>();
  private final List<EventObserver<Annotation, Event>> observers =
      new ArrayList<EventObserver<Annotation, Event>>();

  private Injector injector;

  protected ObserverMap() {}

  /**
   * Each subsequent registration has higher precedence, to match the behavior of
   * {@link EventDispatch#addGlobalDecorator}.
   */
  public void register(AnnotatedElement element) {
    for (Annotation annotation : element.getAnnotations()) {
      EventObserverBinding bindingAnnotation = annotation.annotationType().getAnnotation(
          EventObserverBinding.class);
      if (bindingAnnotation == null) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Provider<EventObserver<Annotation, Event>> provider =
          (Provider<EventObserver<Annotation, Event>>) injector.getProvider(
              bindingAnnotation.value());

      EventObserver<Annotation, Event> observer = provider.get();
      observer.initialize(annotation);

      annotations.add(0, annotation);
      observers.add(0, observer);
    }
    assert annotations.size() == observers.size();
  }

  public boolean shouldFire(final Event event, final Object context) {
    final boolean[] shouldFire = { true };
    Iterator<Annotation> aIt = annotations.iterator();
    Iterator<EventObserver<Annotation, Event>> pIt = observers.iterator();
    while (aIt.hasNext() && pIt.hasNext()) {
      final Annotation annotation = aIt.next();
      EventObserver<Annotation, Event> filter = pIt.next();
      // TODO: FilterScope?

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
          public Object getContext() {
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
          public void suppressEvent() {
            shouldFire[0] = false;
          }
        };

        // Allow all filters to fire
        filter.observeEvent(ctx);
      }
    }
    return shouldFire[0];
  }

  public void shutdown() {
    for (EventObserver<?, ?> observer : observers) {
      observer.shutdown();
    }
    observers.clear();
  }

  @Inject
  void inject(Injector injector) {
    this.injector = injector;
  }
}
