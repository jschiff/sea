package com.getperka.sea.ext;

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

import java.lang.annotation.Annotation;

import com.getperka.sea.CompositeEvent;
import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;

/**
 * An EventObserver is invoked during a call to {@link EventDispatch#fire} to allow {@link Event}
 * instances to be observed and possibly suppressed.
 * <p>
 * An EventObserver operates at a global scope in the context of a single Event <em>instance</em>,
 * as opposed to an {@link EventDecorator}, which operates on one of arbitrarily many receiver
 * method <em>invocations</em> that the Event may trigger.
 * <p>
 * EventObservers are registered by calling {@link EventDispatch#addGlobalDecorator}, passing a
 * class or method that has that is annotated with a binding annotation. Once registered, the
 * {@link #initialize} method will be called to allow the observer to configure itself.
 * 
 * <pre>
 * &#064;EventObserverBinding(MyObserver.class)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
 * &#064;interface Observed {}
 * 
 * class MyObserver implements EventObserver&lt;Observed, SomeEventType&gt; { ... }
 * 
 * &#064;Observed
 * class SomeClass {
 *   void setUp() {
 *     EventDispatch dispatch = EventDispatchers.create();
 *     dispatch.addGlobalDecorator(getClass());
 *   }
 * }
 * </pre>
 * 
 * @param <A> the annotation type used to bind instances of EventObserver
 * @param <E> the expected event type
 */
public interface EventObserver<A extends Annotation, E extends Event> {
  /**
   * Supplies an {@link EventObserver} with information about the current event being dispatched.
   * Instances of Context will be provided to {@link EventObserver#shouldFire} by the dispatch
   * plumbing.
   * <p>
   * This interface is subject to expansion in the future.
   * 
   * @param <A> the annotation type used to bind instances of EventObserver
   * @param <E> the expected event type
   */
  public interface Context<E extends Event> {
    /**
     * Returns additional metadata about the event being dispatched.
     */
    EventContext getContext();

    /**
     * The {@link Event} (or facet of a {@link CompositeEvent}) being dispatched.
     */
    E getEvent();

    /**
     * Returns the {@link Event} that was passed to {@link EventDispatch#fire}. This is usually the
     * same object as returned from {@link #getEvent()}, unless the original event is a
     * {@link CompositeEvent}.
     */
    Event getOriginalEvent();

    /**
     * Calling this method will prevent the Event from being dispatched to any receivers.
     * Suppressing an event does not short-circuit any remaining observers.
     */
    void suppressEvent();
  }

  /**
   * Called by {@link EventDispatch} when the observer is registered.
   * 
   * @param annotation the binding annotation
   */
  void initialize(A annotation);

  /**
   * Process an event before it is distributed to the receivers. Implementations of this method must
   * be thread-safe.
   */
  void observeEvent(Context<E> context);

  /**
   * Called when {@link EventDispatch#shutdown()} is called.
   */
  void shutdown();
}
