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

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;

import com.getperka.sea.CompositeEvent;
import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;

/**
 * An EventDecorator can be used for contextual setup when dispatching events to particular
 * receivers.
 * <p>
 * An implementation of {@link EventDecorator} might look like:
 * 
 * <pre>
 * public class MyEventDecorator&lt;MyBinding, MyEvent&gt; {
 *   public Callable&lt;Object&gt; wrap(final Context ctx) {
 *     return new Callable&lt;Object&gt;() {
 *       public Object call() throws Exception {
 *         // Do some setup
 *         try {
 *           return ctx.getWork().call();
 *         } finally {
 *           // Do some tear-down
 *         }
 *       }
 *     };
 *   }
 * }
 * </pre>
 * 
 * A binding annotation is then declared:
 * 
 * <pre>
 * &#064;EventDecoratorBinding(MyEventDecorator.class)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
 * public @interface NeedsMyDecorator {}
 * </pre>
 * 
 * The decorator is then bound to the receiving method, class, or package:
 * 
 * <pre>
 * &#064;NeedsMyDecorator
 * public class MyReceiver {
 *   &#064;Receiver
 *   public void receive(MyEvent e) {}
 * }
 * </pre>
 * 
 * Receiver-specific configuration can be provided to the decorator by adding properties to the
 * binding annotation:
 * 
 * <pre>
 * &#064;NeedsParameterizedDecorator(foo = &quot;bar&quot;, hello = SomeEnum.WORLD)
 * public class MyOtherReceiver {}
 * </pre>
 * <p>
 * If the dispatched binding annotation or event type are not assignable to the values returned from
 * the getters in this interface, the {@link #wrap} method will not be called. This allows
 * specialized decorators to be applied to a receiver with a wider event supertype.
 * 
 * @param <A> the annotation type used to bind instances of the EventDecorator
 * @param <E> the expected event type
 * @see EventDispatch#addGlobalDecorator
 */
public interface EventDecorator<A extends Annotation, E extends Event> {
  /**
   * Supplies an {@link EventDecorator} with information about the current execution context.
   * Instances of Context will be provided to {@link EventDecorator#wrap} by the dispatch plumbing.
   * <p>
   * This interface may be subject to expansion in the future.
   * 
   * @param <A> the annotation type used to bind instances of the EventDecorator
   * @param <E> the expected event or event facet type
   */
  public interface Context<A extends Annotation, E extends Event> {
    /**
     * Fire the given event once the entire decorator stack has unwound.
     */
    void fireLater(Event event);

    /**
     * The binding annotation that triggered the {@link EventDecorator}.
     */
    A getAnnotation();

    /**
     * Returns the {@link EventContext} containing supplemental information about the {@link Event}.
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
     * Returns the instance the {@link Receiver} method is being invoked upon. If the method being
     * invoked is static, this method will return {@code null}.
     */
    Object getReceiverInstance();

    /**
     * The receiving target.
     */
    ReceiverTarget getTarget();

    /**
     * The work to wrap. The return value of the {@link Callable} will be the return value of the
     * invoked receiver or a replacement value provided by another decorator.
     */
    Callable<Object> getWork();

    /**
     * Prevent any subsequent decorators or the receiver method from being invoked. This method will
     * cause {@link #wasDispatched()} to return {@code true} so that any active decorators can
     * perform post-dispatch tasks. This method may be called from
     * {@link EventDecorator#wrap(Context)} in preference to returning {@code null} or it may be
     * called from within the returned Callable to only partially execute the decorator chain.
     */
    void shortCircuit();

    /**
     * Prevent any subsequent decorators or the receiver method from being invoked. This method will
     * cause {@link #wasDispatched()} to return {@code true} and {@code t} to be returned from
     * {@link #wasThrown()}. The use of this method for error reporting is preferable to throwing
     * exceptions from the Callable returned from {@link EventDecorator#wrap(Context)}.
     * 
     * @param t a Throwable to return from {@link #wasThrown()}
     */
    void shortCircuit(Throwable t);

    /**
     * Returns {@code true} if the {@link ReceiverTarget} was invoked with an {@link Event} (i.e.
     * none of the target's {@link EventDecorator} wrappers returned {@code null}).
     */
    boolean wasDispatched();

    /**
     * Returns {@code true} if the {@link ReceiverTarget} called {@link EventContext#suspend()}.
     * EventDecorators should expect that the event may be re-fired.
     */
    boolean wasSuspended();

    /**
     * Returns the exception that was thrown by the {@link ReceiverTarget}.
     */
    Throwable wasThrown();
  }

  /**
   * Allows custom logic to be applied during an event receiver invocation. An EventDecorator may
   * cancel the dispatch of an event by returning {@code null} from this method. This will also
   * prevent any other EventDecorators in the chain from having their {@code wrap} method called.
   * 
   * @param ctx the event dispatch context
   * @return a callable with appropriate decorations applied
   */
  Callable<Object> wrap(Context<A, E> ctx);
}
