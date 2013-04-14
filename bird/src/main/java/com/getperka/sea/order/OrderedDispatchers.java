package com.getperka.sea.order;

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

import com.getperka.sea.EventDispatch;
import com.getperka.sea.impl.HasInjector;

/**
 * A factory for {@link OrderedDispatch} instances.
 */
public class OrderedDispatchers {
  /**
   * Instantiates a new {@link OrderedDispatch} for a specific {@link EventDispatch}.
   */
  public static OrderedDispatch create(EventDispatch dispatch) {
    return ((HasInjector) dispatch).getInstance(OrderedDispatch.class);
  }

  private OrderedDispatchers() {}
}