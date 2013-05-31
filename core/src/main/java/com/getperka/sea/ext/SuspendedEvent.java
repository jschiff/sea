package com.getperka.sea.ext;

import com.getperka.sea.util.EventWaker;

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

/**
 * A handle to an event whose processing was suspended by its receiver method. Calling
 * {@link #resume()} will invoke the specific receiver method that suspended the event.
 * 
 * @see EventWaker
 */
public interface SuspendedEvent {
  /**
   * Resume the receiver method that suspended the event. This method may be called only once on a
   * specific instance of {@code SuspendedEvent}. If the event is re-suspended, the newly returned
   * {@code SuspendedEvent} must be used to re-resume the event.
   * 
   * @throws IllegalStateException if {@code resume} is called more than once.
   */
  void resume();
}
