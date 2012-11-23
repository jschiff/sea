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



import com.getperka.sea.ext.EventDecorator;

/**
 * A base type for events that should return the result of a computation to its initiators.
 * <p>
 * The {@link Implementation} {@link EventDecorator} is applied to the method responsible for
 * updating the event with the result of the computation. The {@link Implementation} decorator will
 * automatically re-fire the event once the receiver method returns.
 * 
 * <pre>
 * class ProcessesOutcome {
 *   &#064;Receiver
 *   &#064;OutcomeEvent.Implementation
 *   void implementation(MyOutcomeEvent evt) {
 *     evt.setResult(doComputation(evt.getParameter()));
 *   }
 * }
 * </pre>
 * 
 * The {@link Success} an {@link Failure} annotations are used to filter the event when returning it
 * to the
 * 
 * <pre>
 * class WantsOutcome {
 *   public void doIt() {
 *     MyOutcomeEvent evt = new MyOutcomeEvent();
 *     evt.setParameter(&quot;foo&quot;);
 *     eventDispatch.fire(evt);
 *   }
 * 
 *   &#064;Receiver
 *   &#064;OutcomeEvent.Success
 *   void success(MyOutcomeEvent evt) {
 *     evt.getResultOfComputation();
 *   }
 * 
 *   &#064;Receiver
 *   &#064;OutcomeEvent.Failure
 *   void failure(MyOutcomeEvt evt) {
 *     evt.getFailure();
 *   }
 * }
 * </pre>
 */
public interface OutcomeEvent extends TaggedEvent {
  /**
   * Returns the exception thrown by the {@link Implementation} method that processed the event.
   */
  Throwable getFailure();

  /**
   * Returns {@code true} if the {@link Implementation} that processed the event completed
   * successfully.
   */
  boolean isSuccess();

  /**
   * This is normally set by the {@link Implementation} decorator.
   */
  void setFailure(Throwable t);

  /**
   * This is normally set by the {@link Implementation} decorator.
   */
  void setSuccess(boolean success);
}
