package com.getperka.sea.impl;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.DecoratorScoped;

@DecoratorScoped
public class DecoratorContext implements EventDecorator.Context<Annotation, Event> {

  private final Annotation annotation;
  private final Event event;
  private final ReceiverTarget target;
  private final Callable<Object> work;

  @Inject
  protected DecoratorContext(Annotation annotation, Event event, ReceiverTarget target,
      Callable<Object> work) {
    this.annotation = annotation;
    this.event = event;
    this.target = target;
    this.work = work;
  }

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

}
