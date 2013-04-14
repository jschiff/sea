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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import com.getperka.sea.ext.ConfigurationProvider;
import com.getperka.sea.ext.ConfigurationVisitor;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;
import com.getperka.sea.ext.EventObserver;
import com.getperka.sea.ext.EventObserverBinding;
import com.getperka.sea.ext.ExternalBinding;
import com.getperka.sea.ext.ExternalBindings;

/**
 * Manages bindings from annotations to their respective decorators and observers.
 */
@Singleton
public class BindingMap implements ConfigurationProvider {

  private final Map<Class<? extends Annotation>, Class<? extends EventDecorator<?, ?>>> decorators =
      new ConcurrentHashMap<Class<? extends Annotation>, Class<? extends EventDecorator<?, ?>>>();

  private final Map<Class<? extends Annotation>, Class<? extends EventObserver<?, ?>>> observers =
      new ConcurrentHashMap<Class<? extends Annotation>, Class<? extends EventObserver<?, ?>>>();

  protected BindingMap() {}

  @Override
  public void accept(ConfigurationVisitor visitor) {
    for (Map.Entry<Class<? extends Annotation>, Class<? extends EventDecorator<?, ?>>> entry : decorators
        .entrySet()) {
      visitor.decoratorBinding(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<Class<? extends Annotation>, Class<? extends EventObserver<?, ?>>> entry : observers
        .entrySet()) {
      visitor.observerBinding(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Retrieve the {@link EventDecorator} type that should be created for the given annotation, or
   * {@code null} if the annotation does not imply a binding.
   */
  public Class<? extends EventDecorator<?, ?>> getDecorator(Annotation annotation) {
    // Start with any externally-defined annotation
    Class<? extends EventDecorator<?, ?>> decoratorType =
        decorators.get(annotation.annotationType());
    if (decoratorType != null) {
      return decoratorType;
    }

    // Is the annotation itself annotated with a binding annotation?
    EventDecoratorBinding bindingAnnotation =
        annotation.annotationType().getAnnotation(EventDecoratorBinding.class);
    if (bindingAnnotation != null) {
      decoratorType = bindingAnnotation.value();
    }
    return decoratorType;
  }

  /**
   * Retrieve the {@link EventObserver} type that should be created for the given annotation, or
   * {@code null} if the annotation does not imply a binding.
   */
  public Class<? extends EventObserver<?, ?>> getObserver(Annotation annotation) {
    // Start with any externally-defined annotation
    Class<? extends EventObserver<?, ?>> decoratorType = observers.get(annotation.annotationType());
    if (decoratorType != null) {
      return decoratorType;
    }

    // Is the annotation itself annotated with a binding annotation?
    EventObserverBinding bindingAnnotation =
        annotation.annotationType().getAnnotation(EventObserverBinding.class);
    if (bindingAnnotation != null) {
      decoratorType = bindingAnnotation.value();
    }
    return decoratorType;
  }

  /**
   * Register global decorators.
   */
  public void register(AnnotatedElement element) {
    // First look for any external binding declarations
    for (Annotation annotation : element.getAnnotations()) {
      if (!(annotation instanceof ExternalBindings)) {
        continue;
      }
      for (ExternalBinding binding : ((ExternalBindings) annotation).value()) {
        Class<? extends EventDecorator<?, ?>> decorator = binding.decorator();
        if (!NoOpTarget.class.equals(decorator)) {
          decorators.put(binding.annotation(), decorator);
        }

        Class<? extends EventObserver<?, ?>> observer = binding.observer();
        if (!NoOpTarget.class.equals(observer)) {
          observers.put(binding.annotation(), observer);
        }
      }
    }
  }
}
