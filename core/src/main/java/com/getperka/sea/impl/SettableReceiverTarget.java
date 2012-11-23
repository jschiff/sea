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

import javax.inject.Provider;

import com.getperka.sea.ext.ReceiverTarget;

/**
 * A {@link ReceiverTarget} that can be (re-)set.
 */
public interface SettableReceiverTarget extends ReceiverTarget {
  void setInstanceDispatch(Provider<? extends Object> provider, Method method);

  void setStaticDispatch(Method method);
}
