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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.DecoratorScoped;
import com.getperka.sea.inject.WasDispatched;
import com.getperka.sea.inject.WasThrown;

@DecoratorScoped
public class DecoratorContext implements EventDecorator.Context<Annotation, Event> {
  private Annotation annotation;
  private Event event;
  private ReceiverTarget target;
  private AtomicBoolean wasDispatched;
  private AtomicReference<Throwable> wasThrown;
  private Callable<Object> work;

  @Inject
  protected DecoratorContext() {}

  @Override
  public Annotation getAnnotation() {
    return annotation;
  }

  @Override
  public Event getEvent() {
    return event;
  }

  @Override
  public ReceiverTarget getTarget() {
    return target;
  }

  @Override
  public Callable<Object> getWork() {
    return work;
  }

  @Override
  public boolean wasDispatched() {
    return wasDispatched.get();
  }

  @Override
  public Throwable wasThrown() {
    return wasThrown.get();
  }

  @Inject
  void inject(Annotation annotation, Event event, ReceiverTarget target,
      @WasDispatched AtomicBoolean wasDispatched, @WasThrown AtomicReference<Throwable> wasThrown,
      Callable<Object> work) {
    this.annotation = annotation;
    this.event = event;
    this.target = target;
    this.wasDispatched = wasDispatched;
    this.wasThrown = wasThrown;
    this.work = work;
  }
}
