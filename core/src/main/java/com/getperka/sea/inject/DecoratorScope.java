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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.getperka.sea.ext.EventDecorator;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * Defines a scope whose lifetime is a call to {@link EventDecorator#wrap}.
 */
public class DecoratorScope extends BaseScope {
  private final ThreadLocal<Map<Key<?>, Object>> map = new ThreadLocal<Map<Key<?>, Object>>();

  public void enter(Annotation annotation, Callable<Object> work) {
    Map<Key<?>, Object> localMap = map.get();
    if (localMap != null) {
      throw new IllegalStateException("DecoratorScope is not reentrant");
    }
    localMap = new HashMap<Key<?>, Object>();
    map.set(localMap);
    localMap.put(Key.get(Annotation.class), annotation);
    localMap.put(Key.get(new TypeLiteral<Callable<Object>>() {}), work);
  }

  public void exit() {
    map.remove();
  }

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    return new MapProvider<T>(key, unscoped) {
      @Override
      protected Map<Key<?>, Object> scopeMap() {
        Map<Key<?>, Object> localMap = map.get();
        if (localMap == null) {
          throw new IllegalStateException("Not in a DecoratorScope");
        }
        return localMap;
      }
    };
  }
}
