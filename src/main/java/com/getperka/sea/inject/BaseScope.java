package com.getperka.sea.inject;

import com.google.inject.Provider;
import com.google.inject.Scope;

abstract class BaseScope implements Scope {

  static class DummyProvider implements Provider<Object> {
    static final DummyProvider INSTANCE = new DummyProvider();

    @Override
    public Object get() {
      throw new IllegalStateException("Not in an EventScope");
    }
  }

  static class ThreadLocalProvider<T> implements Provider<T> {
    private final ThreadLocal<? extends T> local;

    public ThreadLocalProvider(ThreadLocal<? extends T> local) {
      this.local = local;
    }

    @Override
    public T get() {
      return local.get();
    }
  }

  @SuppressWarnings("unchecked")
  protected <T> Provider<T> cast(Provider<?> provider) {
    return (Provider<T>) provider;
  }

  protected <T> Provider<T> provider() {
    return cast(DummyProvider.INSTANCE);
  }
}
