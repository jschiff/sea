package com.getperka.sea.ext;

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
import java.util.Collections;
import java.util.List;

import com.getperka.sea.Event;

/**
 * A meta-event that indicates that all receivers and their decorators have had an opportunity to
 * process an event. These events may be used to trigger other chained events, perform cleanup, or
 * verify that an Event was actually processed.
 * <p>
 * Instances of this event are automatically fired by the core dispatch code in response to all
 * fired events. They are not fired, however, in response to themselves.
 * <p>
 * A simple global sanity-check for events dropped due to a lack of receivers can be created by
 * looking for DispatchCompletedEvents with a zero-length {@link #getResults()}.
 */
public class DispatchCompleteEvent implements Event {
  private List<DispatchResult> results = Collections.emptyList();
  private Event source;

  /**
   * Returns the DispatchEvents produced from all receivers that received the event.
   */
  public List<DispatchResult> getResults() {
    return results;
  }

  public Event getSource() {
    return source;
  }

  public void setResults(List<DispatchResult> results) {
    this.results = results;
  }

  public void setSource(Event source) {
    this.source = source;
  }

  /**
   * Returns {@code true} if the event was received by at least one receiver method.
   */
  public boolean wasReceived() {
    for (DispatchResult res : results) {
      if (res.wasReceived()) {
        return true;
      }
    }
    return false;
  }
}
