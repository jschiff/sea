package com.getperka.sea.inject;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.getperka.sea.Event;
import com.getperka.sea.ext.ReceiverTarget;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class DecoratorScope extends BaseScope {

  private final ThreadLocal<Annotation> annotation = new ThreadLocal<Annotation>();
  private final ThreadLocal<Event> event = new ThreadLocal<Event>();
  private final ThreadLocal<ReceiverTarget> target = new ThreadLocal<ReceiverTarget>();
  private final ThreadLocal<Callable<Object>> work = new ThreadLocal<Callable<Object>>();
  private final Map<Key<?>, ThreadLocal<?>> map = new HashMap<Key<?>, ThreadLocal<?>>();

  DecoratorScope() {
    map.put(Key.get(Annotation.class), annotation);
    map.put(Key.get(Event.class), event);
    map.put(Key.get(ReceiverTarget.class), target);
    map.put(Key.get(new TypeLiteral<Callable<Object>>() {}), work);
  }

  public void exit() {
    for (ThreadLocal<?> local : map.values()) {
      local.remove();
    }
  }

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    ThreadLocal<?> local = map.get(key);
    if (local != null) {
      return cast(new ThreadLocalProvider<Object>(local));
    }
    return unscoped;
  }

  public DecoratorScope withAnnotation(Annotation annotation) {
    this.annotation.set(annotation);
    return this;
  }

  public DecoratorScope withEvent(Event event) {
    this.event.set(event);
    return this;
  }

  public DecoratorScope withTarget(ReceiverTarget target) {
    this.target.set(target);
    return this;
  }

  public DecoratorScope withWork(Callable<Object> work) {
    this.work.set(work);
    return this;
  }

}
