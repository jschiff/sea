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

import com.getperka.sea.ext.EventDecorator;

class ImplementationFilter implements EventDecorator<Implementation, OutcomeEvent> {
  ImplementationFilter() {}

  @Override
  public Callable<Object> wrap(final Context<Implementation, OutcomeEvent> ctx) {
    OutcomeEvent event = ctx.getEvent();

    // Discard the invocation if the implementation has success or failure info
    boolean hasResult = event.isSuccess() || event.getFailure() != null;
    if (hasResult) {
      return null;
    }

    return new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        OutcomeEvent event = ctx.getEvent();
        try {
          Object toReturn = ctx.getWork().call();
          // Only record data if the underlying ReceiverTarget was invoked.
          if (ctx.wasDispatched()) {
            Throwable thrown = ctx.wasThrown();
            event.setFailure(thrown);
            event.setSuccess(thrown == null);
          }
          return toReturn;
        } finally {
          // Optionally re-dispatch the event to trigger @Success / @Failure receivers
          if (ctx.wasDispatched() && ctx.getAnnotation().fireResult()) {
            ctx.fireLater(ctx.getOriginalEvent());
          }
        }
      }
    };
  }
}