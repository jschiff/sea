package com.getperka.sea;

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

import com.getperka.sea.inject.EventModule;
import com.google.inject.Guice;

/**
 * A factory for {@link EventDispatch} instances.
 */
public class EventDispatchers {
  /**
   * Instantiates a new {@link EventDispatch} instance.
   */
  public static EventDispatch create() {
    return Guice.createInjector(new EventModule()).getInstance(EventDispatch.class);
  }

  private EventDispatchers() {}
}