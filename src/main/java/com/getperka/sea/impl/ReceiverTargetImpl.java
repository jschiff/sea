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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.inject.Provider;

/**
 * Encapsulates a method and an instance on which to execute it.
 */
public class ReceiverTargetImpl implements SettableReceiverTarget {
  private Provider<?> instanceProvider;
  private Method method;

  protected ReceiverTargetImpl() {}

  /**
   * Only calls {@link #instanceProvider} once.
   */
  @Override
  public synchronized Object getInstance() {
    if (instanceProvider == null) {
      return null;
    }
    return instanceProvider.get();
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public void setInstanceDispatch(Provider<?> provider, Method method) {
    this.instanceProvider = provider;
    this.method = method;
  }

  @Override
  public void setStaticDispatch(Method staticMethod) {
    if (!Modifier.isStatic(staticMethod.getModifiers())) {
      throw new IllegalArgumentException();
    }
    instanceProvider = null;
    method = staticMethod;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(method.getDeclaringClass().getName()).append(".")
        .append(method.getName()).append("(");
    boolean needsComma = false;
    for (Class<?> clazz : method.getParameterTypes()) {
      if (needsComma) {
        sb.append(", ");
      } else {
        needsComma = true;
      }
      sb.append(clazz.getName());
    }
    sb.append(")").append(method.getReturnType().getName());
    return sb.toString();
  }
}
