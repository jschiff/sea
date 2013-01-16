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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.ext.DecoratorOrder;
import com.getperka.sea.ext.EventDecorator;
import com.google.inject.Injector;

/**
 * Holds global bindings and calculates the decorators that should be used when dispatching events.
 */
@Singleton
public class DecoratorMap {
  /**
   * Extract all annotations declared on the element, partially sorted by a {@link DecoratorOrder}.
   */
  static List<Annotation> orderedAnnotations(AnnotatedElement elt) {
    Set<Annotation> orderedAnnotations = new LinkedHashSet<Annotation>();
    DecoratorOrder order = elt.getAnnotation(DecoratorOrder.class);
    if (order == null) {
      return Arrays.asList(elt.getDeclaredAnnotations());
    }
    for (Class<? extends Annotation> lookFor : order.value()) {
      Annotation a = elt.getAnnotation(lookFor);
      if (a != null) {
        orderedAnnotations.add(a);
      }
    }
    orderedAnnotations.addAll(Arrays.asList(elt.getDeclaredAnnotations()));
    return new ArrayList<Annotation>(orderedAnnotations);
  }

  private BindingMap bindingMap;
  private final List<Annotation> globalAnnotations = new ArrayList<Annotation>();
  /**
   * Prevents concurrent modification of {@link #globalAnnotations} and {@link #globalProviders}.
   */
  private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
  private final List<Provider<EventDecorator<Annotation, Event>>> globalProviders =
      new ArrayList<Provider<EventDecorator<Annotation, Event>>>();
  private Injector injector;

  protected DecoratorMap() {}

  /**
   * Given a method, compute the decorators and configuration annotations. The accumulator lists
   * will have the same number of elements added to them.
   * 
   * @param method the method to be decorated
   * @param annotations an accumulator for the annotations to pass to the decorators
   * @param providers an accumulator for the the decorators to invoke
   */
  public void computeDecorators(Method method, List<Annotation> annotations,
      List<Provider<EventDecorator<Annotation, Event>>> providers) {

    compute(method, annotations, providers);
    compute(method.getDeclaringClass(), annotations, providers);
    compute(method.getDeclaringClass().getPackage(), annotations, providers);
    globalLock.readLock().lock();
    try {
      annotations.addAll(globalAnnotations);
      providers.addAll(globalProviders);
    } finally {
      globalLock.readLock().unlock();
    }
  }

  /**
   * Register global decorators.
   */
  public void register(AnnotatedElement element) {
    globalLock.writeLock().lock();
    try {
      compute(element, globalAnnotations, globalProviders);
      assert globalAnnotations.size() == globalProviders.size();
    } finally {
      globalLock.writeLock().unlock();
    }
  }

  @Inject
  void inject(BindingMap bindingMap, Injector injector) {
    this.bindingMap = bindingMap;
    this.injector = injector;
  }

  private void compute(AnnotatedElement elt, List<Annotation> annotations,
      List<Provider<EventDecorator<Annotation, Event>>> providers) {
    /*
     * Get a partially-ordered list of annotations, then reverse it so that the user's
     * highest-priority decorators are applied last (and therefore their wrapped callable is
     * executed first).
     */
    List<Annotation> orderedAnnotations = orderedAnnotations(elt);
    Collections.reverse(orderedAnnotations);

    for (Annotation annotation : orderedAnnotations) {
      // Start with any externally-defined annotation
      Class<? extends EventDecorator<?, ?>> decoratorType =
          bindingMap.getDecorator(annotation);

      // If there's no decorator associated with the annotation, continue on
      if (decoratorType == null) {
        continue;
      }

      // Get a provider for the decorator
      @SuppressWarnings("unchecked")
      Provider<EventDecorator<Annotation, Event>> provider =
          (Provider<EventDecorator<Annotation, Event>>) injector.getProvider(decoratorType);

      annotations.add(annotation);
      providers.add(provider);
    }
  }
}
