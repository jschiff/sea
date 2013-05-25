package com.getperka.sea.decoration;
/*
 * #%L
 * Simple Event Architecture - Core
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Singleton;

import com.getperka.sea.ext.EventContext;

/**
 * This prevents a {@link Success} or {@link Failure} event from being fired in the same event tick
 * as the {@link Implementation}.
 */
@Singleton
class OutcomeEventCoordinator {

  private final Map<OutcomeEvent, Long> sequenceNumbers =
      Collections.synchronizedMap(new WeakHashMap<OutcomeEvent, Long>());

  /**
   * Return {@code true} if the current event sequence number differs from the one used for the
   * implementation. This method will also return {@code true} if no prior implementation was
   * recorded, since the event may have been implemented elsewhere.
   */
  public boolean mayFollowUp(OutcomeEvent evt, EventContext context) {
    Long priorRun = sequenceNumbers.get(evt);
    return priorRun == null || priorRun.longValue() != context.getSequenceNumber();
  }

  /**
   * Record the current sequence number for the event.
   * 
   * @return {@code true} if the event had not been previously recorded
   */
  public boolean recordImplementation(OutcomeEvent evt, EventContext context) {
    return sequenceNumbers.put(evt, context.getSequenceNumber()) == null;
  }
}
