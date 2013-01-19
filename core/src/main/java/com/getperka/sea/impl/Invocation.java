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
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.EventLogger;

/**
 * The top-level invocation of a {@link ReceiverTarget ReceiverTarget's} work. This class sets the
 * name of the current thread and provides unhandled exception dispatch for exceptions that occur
 * within the decorator / dispatch plumbing.
 */
public class Invocation implements Callable<DispatchResult> {
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

  private Object context;
  private EventDispatch dispatch;
  private Event event;
  private Logger logger;
  private InvocationManager manager;
  private ReceiverTarget target;
  private State state;

  protected Invocation() {}

  @Override
  public DispatchResult call() {
    logger.trace("Invocation starting: {}", this);
    DispatchResult toReturn = null;
    Thread.currentThread().setName(toString());
    try {
      toReturn = target.dispatch(event, context);
    } catch (Throwable t) {
      logger.error("Unable to dispatch event", t);
    } finally {
      maybeDispatchCompleteEvent(toReturn);
      Thread.currentThread().setName("idle");
      manager.markComplete(this);
    }
    return toReturn;
  }

  public void setContext(Object context) {
    this.context = context;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public void setInvocation(ReceiverTarget target) {
    this.target = target;
  }

  public void setState(State state) {
    this.state = state;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return target.toString();
  }

  @Inject
  void inject(EventDispatch dispatch, InvocationManager manager, @EventLogger Logger logger) {
    this.dispatch = dispatch;
    this.manager = manager;
    this.logger = logger;
  }

  private void maybeDispatchCompleteEvent(DispatchResult toReturn) {
    if (state.isLastInvocation(toReturn) && !(event instanceof DispatchCompleteEvent)) {
      DispatchCompleteEvent complete = new DispatchCompleteEvent();
      complete.setSource(event);
      complete.setResults(new ArrayList<DispatchResult>(state.getResults()));
      dispatch.fire(complete);
    }
  }
}
