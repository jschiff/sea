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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

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
public interface OutcomeEvent extends Event {
  /**
   * A base class that can be used when implementing {@link OutcomeEvent}.
   */
  public class Base implements OutcomeEvent {
    private Throwable failure;
    private boolean success;

    @Override
    public Throwable getFailure() {
      return failure;
    }

    @Override
    public boolean isSuccess() {
      return success;
    }

    @Override
    public void setFailure(Throwable failure) {
      this.failure = failure;
    }

    @Override
    public void setSuccess(boolean success) {
      this.success = success;
    }
  }

  /**
   * A receiver that should receive an {@link OutcomeEvent} only if
   * {@link OutcomeEvent#getFailure()} is non-null.
   * 
   * @see OutcomeEvent
   */
  @Documented
  @EventDecoratorBinding(FailureFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
  public @interface Failure {}

  static class FailureFilter implements EventDecorator<Failure, OutcomeEvent> {

    @Override
    public Callable<Object> wrap(Context<Failure, OutcomeEvent> ctx) {
      return ctx.getEvent().getFailure() == null ? null : ctx.getWork();
    }
  }

  /**
   * The receiver responsible for updating the {@link OutcomeEvent} with the result of the
   * computation.
   * 
   * @see OutcomeEvent
   */
  @Documented
  @EventDecoratorBinding(ImplementationFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
  public @interface Implementation {
    /**
     * By default, the {@link Implementation} decorator will re-fire the {@link OutcomeEvent} to the
     * current {@link EventDispatch} once the receiver method exits. Setting this property to
     * {@code false} will disable this behavior.
     */
    boolean fireResult() default true;
  }

  static class ImplementationFilter implements EventDecorator<Implementation, OutcomeEvent> {
    private final EventDispatch dispatch;

    @Inject
    ImplementationFilter(EventDispatch dispatch) {
      this.dispatch = dispatch;
    }

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
              dispatch.fire(event);
            }
          }
        }
      };
    }
  }

  /**
   * A receiver that should receive an {@link OutcomeEvent} only if {@link OutcomeEvent#isSuccess()}
   * returns {@code true}.
   * 
   * @see OutcomeEvent
   */
  @Documented
  @EventDecoratorBinding(SuccessFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
  public @interface Success {}

  static class SuccessFilter implements EventDecorator<Success, OutcomeEvent> {
    @Override
    public Callable<Object> wrap(Context<Success, OutcomeEvent> ctx) {
      return ctx.getEvent().isSuccess() ? ctx.getWork() : null;
    }
  }

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
