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
   * @param <E> the expected event type
   */
  public interface Context<A extends Annotation, E extends Event> {
    /**
     * The binding annotation that triggered the {@link EventDecorator}.
     */
    A getAnnotation();

    /**
     * The {@link Event} being dispatched.
     */
    E getEvent();

    /**
     * Returns the instance the {@link Receiver} method is being invoked upon. If the method being
     * invoked is static, this method will return an arbitrary object.
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
     * Returns {@code true} if the {@link ReceiverTarget} was invoked with an {@link Event} (i.e.
     * none of the target's {@link EventDecorator} wrappers returned {@code null}).
     */
    boolean wasDispatched();

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
