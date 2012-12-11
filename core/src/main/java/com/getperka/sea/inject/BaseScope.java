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

import java.util.Map;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

public abstract class BaseScope implements Scope {
  protected static abstract class MapProvider<T> implements Provider<T> {
    private final Key<?> key;
    private final Provider<T> unscoped;

    public MapProvider(Key<?> key, Provider<T> unscoped) {
      this.key = key;
      this.unscoped = unscoped;
    }

    @Override
    public T get() {
      Map<Key<?>, Object> map = scopeMap();
      Object toReturn = map.get(key);
      if (toReturn == null) {
        toReturn = unscoped.get();
        if (toReturn == null) {
          toReturn = NULL;
        }
        map.put(key, toReturn);
      }
      @SuppressWarnings("unchecked")
      T toReturnT = toReturn == NULL ? null : (T) toReturn;
      return toReturnT;
    }

    protected abstract Map<Key<?>, Object> scopeMap();
  }

  class DummyProvider<T> implements Provider<T> {
    @Override
    public T get() {
      throw new OutOfScopeException("Not in " + BaseScope.this.getClass().getSimpleName());
    }
  }

  private final DummyProvider<Object> dummyProvider = new DummyProvider<Object>();

  protected static final Object NULL = new Object();

  /**
   * Returns a dummy Provider instance that throws an exception when called.
   */
  public <T> Provider<T> provider() {
    return cast(dummyProvider);
  }

  @SuppressWarnings("unchecked")
  protected <T> Provider<T> cast(Provider<?> provider) {
    return (Provider<T>) provider;
  }
}
