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
import java.util.concurrent.Callable;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;

/**
 * Ensures that events are dispatched to a {@link ReceiverTarget} in a specific order.
 */
class Decorator implements EventDecorator<Ordered, Event> {

  private final EventDispatch dispatch;
  private final OrderedDispatchImpl impl;

  @Inject
  Decorator(EventDispatch dispatch, OrderedDispatchImpl impl) {
    this.dispatch = dispatch;
    this.impl = impl;
  }

  @Override
  public Callable<Object> wrap(final Context<Ordered, Event> ctx) {
    Event event = ctx.getEvent();
    BatchState state = impl.getState(event);

    // Allow unordered events to pass through
    if (state == null) {
      return ctx.getWork();
    }

    return new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        Event event = ctx.getEvent();
        ReceiverTarget target = ctx.getTarget();
        ReceiverState receiverState = impl.getState(event).getState(target);

        /*
         * First, determine if the current Event should be the next Event to be dispatched to the
         * current ReceiverTarget. If not, just return null, since the event should be re-dispatched
         * via the list of Events in ReceiverState.
         */
        if (!receiverState.isNext(event)) {
          return null;
        }

        try {
          // Call the underling work, return the result
          return ctx.getWork().call();
        } finally {
          /*
           * Check to see if the ReceiverTarget was actually called and dispatch the next event that
           * the ReceiverTarget should see. If another decorator has canceled the event's dispatch,
           * push the current Event back into the ReceiverState so that it may be dispatched again.
           */
          if (ctx.wasDispatched()) {
            Event next = receiverState.getNext();
            if (next != null) {
              dispatch.fire(next);
            }
          } else {
            receiverState.pushBack(event);
          }
        }
      }
    };
  }
}