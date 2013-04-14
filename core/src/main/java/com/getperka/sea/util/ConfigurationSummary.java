package com.getperka.sea.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.List;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.ConfigurationVisitor;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventObserver;

/**
 * Produces a textual summary of the current configuration of an {@link EventDispatch}.
 */
public class ConfigurationSummary extends ConfigurationVisitor {

  private Formatter f = new Formatter();

  @Override
  public void decoratorBinding(Class<? extends Annotation> annotation,
      Class<? extends EventDecorator<?, ?>> decorator) {
    f.format("%s -> %s", annotation.getSimpleName(), decorator.getSimpleName());
  }

  @Override
  public void observer(Annotation annotation, EventObserver<?, ?> observer) {
    f.format("%s -> %s:\n", annotation, observer);
  }

  @Override
  public void observerBinding(Class<? extends Annotation> annotation,
      Class<? extends EventObserver<?, ?>> observer) {
    f.format("%s -> %s\n", annotation.getSimpleName(), observer.getSimpleName());
  }

  @Override
  public void receiverMethod(Method method, Class<? extends Event> event,
      List<Annotation> annotations) {
    f.format("%s:\n", event.getSimpleName());
    for (Annotation a : annotations) {
      f.format("  %s\n", a.toString());
    }
    f.format("  %s.%s\n", method.getDeclaringClass().getSimpleName(), method.getName());
  }

  @Override
  public String toString() {
    return f.toString();
  }

}
