package com.getperka.sea.ext;
/*
 * #%L
 * Simple Event Architecture - Core
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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
import java.util.List;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;

/**
 * Provides a formal interface for examining the state of an {@link EventDispatch} and friends. The
 * methods in this class are subject to being called in arbitrary order. Additionally, certain
 * {@link EventDecorator} or {@link EventObserver} implementations may look for additional
 * capability interfaces on the ConfigurationVisitor to provide additional information.
 */
public class ConfigurationVisitor {
  public void decoratorBinding(Class<? extends Annotation> annotation,
      Class<? extends EventDecorator<?, ?>> decorator) {}

  public void endConfiguration() {}

  public void eventDispatch(EventDispatch dispatch) {}

  public void observer(Annotation annotation, EventObserver<?, ?> observer) {}

  public void observerBinding(Class<? extends Annotation> annotation,
      Class<? extends EventObserver<?, ?>> observer) {}

  public void receiverMethod(Method method, Class<? extends Event> event,
      List<Annotation> annotations) {}
}
