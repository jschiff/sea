package com.getperka.sea.ext;

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

import com.getperka.sea.EventDispatch;

/**
 * Encapsulates metadata about a specific event and how it was dispatched.
 */
public interface EventContext {
  /**
   * Returns a monotonically-increasing value.
   */
  long getSequenceNumber();

  /**
   * Returns the user object provided to {@link EventDispatch#fire(com.getperka.sea.Event, Object)

   */
  Object getUserObject();

  /**
   * Indicates that the receiver method does not wish to completely process the event. Calling
   * {@link SuspendedEvent#resume() resume()} on the returned object will invoke the receiver method
   * that called {@code suspend}.
   * <p>
   * Suspending an event in one receiver does not affect dispatch to other receivers that may also
   * receive the event, however it will delay any {@link DispatchCompleteEvent} until the event is
   * resumed.
   * <p>
   * It is the responsibility of any {@link EventDecorator} instances in the dispatch stack to
   * properly release resources and save any temporary state associated with the event.
   * 
   * @see EventDecorator.Context#wasSuspended()
   */
  SuspendedEvent suspend();
}
