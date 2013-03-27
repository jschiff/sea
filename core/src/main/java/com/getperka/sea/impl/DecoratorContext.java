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

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.DecoratorScoped;

@DecoratorScoped
public class DecoratorContext implements EventDecorator.Context<Annotation, Event> {
  @Inject
  EventContext context;
  @Inject
  @CurrentEvent
  Event event;
  @Inject
  ReceiverTarget target;
  @Inject
  ReceiverMethodInvocation invocation;
  @Inject
  @CurrentEvent
  Event originalEvent;

  private Annotation annotation;
  private Callable<Object> work;

  /**
   * Requires injection.
   */
  protected DecoratorContext() {}

  /**
   * Store additional information in the DecoratorContext
   */
  public void configure(Annotation annotation, Event event, Callable<Object> work) {
    this.annotation = annotation;
    this.event = event;
    this.work = work;
  }

  @Override
  public void fireLater(Event event) {
    invocation.getDeferredEvents().add(event);
  }

  @Override
  public Annotation getAnnotation() {
    return annotation;
  }

  @Override
  public EventContext getContext() {
    return context;
  }

  @Override
  public Event getEvent() {
    return event;
  }

  @Override
  public Event getOriginalEvent() {
    return originalEvent;
  }

  @Override
  public Object getReceiverInstance() {
    return invocation.getReceiverInstance();
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
  public void shortCircuit() {
    invocation.shortCircuit(null);
  }

  @Override
  public void shortCircuit(Throwable t) {
    invocation.shortCircuit(t);
  }

  @Override
  public boolean wasDispatched() {
    return invocation.getWasDispatched();
  }

  @Override
  public Throwable wasThrown() {
    return invocation.getWasThrown();
  }
}
