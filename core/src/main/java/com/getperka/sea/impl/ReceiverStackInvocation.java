package com.getperka.sea.impl;

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

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.ReceiverScope;

/**
 * The top-level invocation of a {@link ReceiverTarget ReceiverTarget's} work, including all
 * {@link EventDecorator EventDecorators}. This class sets the name of the current thread and
 * provides unhandled exception dispatch for exceptions that occur within the decorator / dispatch
 * plumbing.
 * <p>
 * Instances of this class should be obtained from {@link InvocationManager#getInvocations}.
 */
public class ReceiverStackInvocation implements Callable<DispatchResult> {
  public static class Unscoped extends ReceiverStackInvocation {
    protected Unscoped() {}
  }

  /**
   * State that is shared between Invocation instances to provide a global view of an event's
   * dispatch metadata.
   */
  static class State {
    private final AtomicInteger invocationsRemaining;
    private final Queue<DispatchResult> results = new ConcurrentLinkedQueue<DispatchResult>();

    public State(int invocationsRemaining) {
      this.invocationsRemaining = new AtomicInteger(invocationsRemaining);
    }

    public Queue<DispatchResult> getResults() {
      return results;
    }

    public boolean isLastInvocation(DispatchResult result) {
      if (result != null) {
        results.add(result);
      }
      return invocationsRemaining.decrementAndGet() == 0;
    }
  }

  private EventContext context;
  @Inject
  private EventDispatch dispatch;
  private Event event;
  @EventLogger
  @Inject
  private Logger logger;
  @Inject
  private InvocationManager manager;
  @Inject
  private ReceiverScope receiverScope;
  private ReceiverTarget target;
  private State state;

  protected ReceiverStackInvocation() {}

  @Override
  public DispatchResult call() {
    logger.trace("Invocation starting: {}", this);
    Thread currentThread = Thread.currentThread();
    String name = currentThread.getName();
    currentThread.setName(toString());

    receiverScope.enter(this, event, target, context);
    DispatchResult toReturn = null;
    try {
      // Figure out a better ReceiverTarget interface to not need this cast
      toReturn = ((ReceiverTargetImpl) target).dispatch(event, context);
    } catch (Throwable t) {
      logger.error("Unable to dispatch event", t);
    } finally {
      receiverScope.exit();
      currentThread.setName(name);
      // If the event was suspended, pretend like it never happened
      if (!toReturn.wasSuspended()) {
        maybeDispatchCompleteEvent(toReturn);
        manager.markComplete(this);
      }
    }
    return toReturn;
  }

  public boolean isSynchronous() {
    return target.isSynchronous();
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return target.toString();
  }

  void setContext(EventContext context) {
    this.context = context;
  }

  void setEvent(Event event) {
    this.event = event;
  }

  void setReceiverTarget(ReceiverTarget target) {
    this.target = target;
  }

  void setState(State state) {
    this.state = state;
  }

  private void maybeDispatchCompleteEvent(DispatchResult toReturn) {
    if (state.isLastInvocation(toReturn) && !(event instanceof DispatchCompleteEvent)) {
      DispatchCompleteEvent complete = new DispatchCompleteEvent();
      complete.setContext(context);
      complete.setSource(event);
      complete.setResults(new ArrayList<DispatchResult>(state.getResults()));
      dispatch.fire(complete);
    }
  }
}
