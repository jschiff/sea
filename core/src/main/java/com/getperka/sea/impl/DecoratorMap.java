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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
   * An immutable pair association a provider for an {@link EventDecorator} and an
   * {@link Annotation} that can be used to configure it.
   */
  public static class DecoratorInfo {
    private final Annotation annotation;
    private final Provider<EventDecorator<Annotation, Event>> provider;

    public DecoratorInfo(Annotation annotation,
        Provider<EventDecorator<Annotation, Event>> provider) {
      this.annotation = annotation;
      this.provider = provider;
    }

    public Annotation getAnnotation() {
      return annotation;
    }

    public Provider<EventDecorator<Annotation, Event>> getProvider() {
      return provider;
    }
  }

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
  private final Map<Method, List<DecoratorInfo>> cache = new ConcurrentHashMap<Method, List<DecoratorInfo>>();
  private final List<DecoratorInfo> globalDecorators = new CopyOnWriteArrayList<DecoratorInfo>();
  private Injector injector;

  protected DecoratorMap() {}

  /**
   * Given a method, compute the decorators and configuration annotations.
   * 
   * @param method the method to be decorated
   * @return a list of {@link DecoratorInfo} in the order in which the decorators should be applied
   */
  public List<DecoratorInfo> getDecoratorInfo(Method method) {
    List<DecoratorInfo> info = cache.get(method);
    if (info == null) {
      info = Collections.unmodifiableList(computeDecorators(method));
      cache.put(method, info);
    }
    return info;
  }

  /**
   * Register global decorators.
   */
  public void register(AnnotatedElement element) {
    globalDecorators.addAll(compute(element));
    cache.clear();
  }

  @Inject
  void inject(BindingMap bindingMap, Injector injector) {
    this.bindingMap = bindingMap;
    this.injector = injector;
  }

  private List<DecoratorInfo> compute(AnnotatedElement elt) {
    List<DecoratorInfo> toReturn = new ArrayList<DecoratorInfo>();
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

      toReturn.add(new DecoratorInfo(annotation, provider));
    }

    return toReturn;
  }

  private List<DecoratorInfo> computeDecorators(Method method) {
    List<DecoratorInfo> toReturn = new ArrayList<DecoratorInfo>();

    toReturn.addAll(compute(method));
    toReturn.addAll(compute(method.getDeclaringClass()));
    toReturn.addAll(compute(method.getDeclaringClass().getPackage()));
    toReturn.addAll(globalDecorators);

    /*
     * If the same decorator annotation has been applied multiple times, use the global-most
     * ordering, but the receiver-most annotation value. This allows global defaults to be set that
     * can be overridden on a per-receiver basis.
     */
    Map<Class<?>, DecoratorInfo> infoByAnnotationType =
        new LinkedHashMap<Class<?>, DecoratorMap.DecoratorInfo>();

    for (DecoratorInfo info : toReturn) {
      Class<?> annotationType = info.annotation.annotationType();
      // Move any existing info to the back of the line
      DecoratorInfo previous = infoByAnnotationType.remove(annotationType);
      if (previous != null) {
        info = previous;
      }
      infoByAnnotationType.put(annotationType, info);
    }

    toReturn = new ArrayList<DecoratorInfo>(infoByAnnotationType.values());

    return toReturn;
  }
}
