package com.getperka.sea.decoration;
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

import java.util.concurrent.Callable;

import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.EventDecorator;

/**
 * Implementation for {@link Dispatched}.
 */
class DispatchedFilter implements EventDecorator<Dispatched, DispatchCompleteEvent> {
  @Override
  public Callable<Object> wrap(Context<Dispatched, DispatchCompleteEvent> ctx) {
    Dispatched annotation = ctx.getAnnotation();
    DispatchCompleteEvent evt = ctx.getEvent();

    if (!annotation.eventType().isAssignableFrom(evt.getSource().getClass())) {
      return null;
    }

    if (annotation.onlyReceived() && !evt.wasReceived()) {
      return null;
    }

    if (annotation.onlyUnreceived() && evt.wasReceived()) {
      return null;
    }

    return ctx.getWork();
  }
}
