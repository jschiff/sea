package com.getperka.sea;

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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import com.google.inject.Injector;

/**
 * A utility base class for implementing {@link CompositeEvent} types.
 * <p>
 * Subclasses of BaseCompoundEvent may be annotated with a {@link DefaultFacets} annotation to
 * pre-populate the facets for an instance.
 */
public class BaseCompositeEvent implements CompositeEvent {
  /**
   * Allows a default collection of facets to be applied to a {@link BaseCompositeEvent}. All facet
   * types must have a public, no-arg constructor.
   */
  @Documented
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface DefaultFacets {
    Class<? extends Event>[] value();
  }

  /**
   * Examines the event and its collection of facets and returns the first one assignable to the
   * requested type. If there are no currently-registered facets of the requested type, this method
   * will return {@code null}.
   */
  public static <E extends Event> E asEventFacet(Class<E> eventType, Event event) {
    if (eventType.isInstance(event)) {
      return eventType.cast(event);
    }
    if (event instanceof CompositeEvent) {
      for (Event evt : ((CompositeEvent) event).getEventFacets()) {
        if (eventType.isInstance(evt)) {
          return eventType.cast(evt);
        }
      }
    }
    return null;

  }

  private Collection<Event> facets;
  @Inject
  private Injector injector;

  /**
   * Delegates to {@link #asEventFacet(Class, Event)}.
   */
  public <E extends Event> E asEventFacet(Class<E> eventType) {
    return asEventFacet(eventType, this);
  }

  @Override
  public synchronized Collection<Event> getEventFacets() {
    if (facets == null) {
      facets = new ArrayList<Event>();

      DefaultFacets defaults = getClass().getAnnotation(DefaultFacets.class);
      if (defaults != null) {
        for (Class<? extends Event> clazz : defaults.value()) {
          Event facet;

          if (injector == null) {
            try {
              facet = clazz.newInstance();
            } catch (IllegalAccessException e) {
              throw new RuntimeException("The type " + clazz.getName()
                + " does not have a public, no-arg constructor", e);
            } catch (InstantiationException e) {
              throw new RuntimeException("Could not construct an instance of " + clazz.getName(), e);
            }
          } else {
            facet = injector.getInstance(clazz);
          }
          facets.add(facet);
        }
      }
    }
    return facets;
  }

  public synchronized void setEventFacets(Collection<? extends Event> facets) {
    this.facets = new ArrayList<Event>(facets);
  }

  /**
   * Adds an Event facet.
   */
  protected void addEventFacet(Event facet) {
    getEventFacets().add(facet);
  }
}
