package com.getperka.sea.inject;
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