package com.getperka.sea.ext;

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
