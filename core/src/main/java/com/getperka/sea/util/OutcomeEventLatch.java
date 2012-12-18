package com.getperka.sea.util;

/*
 * #%L
 * Simple Event Architecture - Core
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.Failure;
import com.getperka.sea.decoration.OutcomeEvent;
import com.getperka.sea.decoration.Success;

/**
 * A specialization of {@link EventLatch} for handling success/failure pairs of {@link OutcomeEvent}
 * dispatches.
 * <p>
 * Instances of this class should not be shared between threads.
 */
public final class OutcomeEventLatch {

  public static OutcomeEventLatch create(EventDispatch dispatch) {
    return new OutcomeEventLatch(dispatch);
  }

  private final EventLatch<OutcomeEvent> latch;
  private final AtomicReference<OutcomeEvent> waitFor = new AtomicReference<OutcomeEvent>();

  @Inject
  OutcomeEventLatch(EventDispatch dispatch) {
    latch = new EventLatch<OutcomeEvent>(dispatch) {
      @Failure
      @Receiver
      void failure(OutcomeEvent evt) {
        if (waitFor.get().equals(evt)) {
          countDown(evt);
        }
      }

      @Receiver
      @Success
      void success(OutcomeEvent evt) {
        if (waitFor.get().equals(evt)) {
          countDown(evt);
        }
      }
    };
  }

  /**
   * Fire the given event and wait for the result of an equivalent event.
   * 
   * @param <T> the type of OutcomeEvent to operate on
   * @param event the event to fire
   * @param duration the maximum number of time units to wait
   * @param unit the length of {@code duration}
   * @return an event that {@link Object#equals(Object) equals} {@code event} or {@code null} if an
   *         outcome was not received within the requested period or the thread was interrupted
   */
  @SuppressWarnings("unchecked")
  public <T extends OutcomeEvent> T awaitOutcome(T event, long duration, TimeUnit unit) {
    waitFor.set(event);
    try {
      return (T) latch.awaitSingleEventAfter(event, duration, unit);
    } finally {
      waitFor.set(null);
    }
  }
}
