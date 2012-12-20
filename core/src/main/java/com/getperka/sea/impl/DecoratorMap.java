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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.google.inject.Injector;

/**
 * Holds global bindings and calculates the decorators that should be used when dispatching events.
 */
@Singleton
public class DecoratorMap {
  private BindingMap bindingMap;
  private final List<Annotation> globalAnnotations = new ArrayList<Annotation>();
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
    annotations.addAll(globalAnnotations);
    providers.addAll(globalProviders);
  }

  /**
   * Register global decorators.
   */
  public void register(AnnotatedElement element) {
    compute(element, globalAnnotations, globalProviders);
    assert globalAnnotations.size() == globalProviders.size();
  }

  @Inject
  void inject(BindingMap bindingMap, Injector injector) {
    this.bindingMap = bindingMap;
    this.injector = injector;
  }

  private void compute(AnnotatedElement elt, List<Annotation> annotations,
      List<Provider<EventDecorator<Annotation, Event>>> providers) {
    // Examine each annotation on the incoming element
    for (Annotation annotation : elt.getDeclaredAnnotations()) {
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
