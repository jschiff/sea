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

import com.getperka.sea.Event;
import com.getperka.sea.Receiver;

/**
 * Provides information about the disposition of a call to
 * {@link ReceiverTarget#dispatch(com.getperka.sea.Event)}.
 */
public interface DispatchResult {

  /**
   * The event that was dispatched.
   */
  Event getEvent();

  /**
   * Returns the value returned by the {@link Receiver} method. This method will return {@code null}
   * if {{@link #wasReceived()} is {@code false}.
   */
  Object getReturnValue();

  /**
   * The receiver that the event was dispatched to.
   */
  ReceiverTarget getTarget();

  /**
   * Returns any exception thrown by the receiving method.
   */
  Throwable getThrown();

  /**
   * Returns {@code true} if the receiver method was actually called.
   */
  boolean wasReceived();

  /**
   * Returns {@code true} if the receiver method called {@link EventContext#suspend()}.
   */
  boolean wasSuspended();

}