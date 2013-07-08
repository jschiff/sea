package com.getperka.sea.impl;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.ReceiverScope;

/**
 * Manages Invocation instances.
 */
@Singleton
public class InvocationManager {
  @Inject
  private EventDispatch dispatch;
  private AtomicBoolean isDraining = new AtomicBoolean();
  @Inject
  private Provider<ReceiverStackInvocation.Unscoped> invocations;
  @EventLogger
  @Inject
  private Logger logger;
  @Inject
  private DispatchMap map;
  private final AtomicInteger pendingInvocations = new AtomicInteger();
  private final Lock pendingLock = new ReentrantLock();
  private final Condition pendingLockCondition = pendingLock.newCondition();
  @Inject
  private ReceiverScope receiverScope;

  protected InvocationManager() {}

  public List<ReceiverStackInvocation> getInvocations(Event event, EventContext context) {

    // Get the list of receiver methods to invoke
    List<ReceiverTarget> targets = map.getTargets(event.getClass());

    // Update bookkeeping information, possibly returning early if invocations should be drained
    pendingLock.lock();
    try {
      if (isDraining.get()) {
        return Collections.emptyList();
      }
      pendingInvocations.addAndGet(targets.size());
    } finally {
      pendingLock.unlock();
    }

    List<ReceiverStackInvocation> toReturn = new ArrayList<ReceiverStackInvocation>();

    // Fire an empty DispatchComplete if there are no receivers
    if (targets.isEmpty() && !(event instanceof DispatchCompleteEvent)) {
      logger.debug("No @Receiver methods that accept {} have been registered",
          event.getClass().getName());

      DispatchCompleteEvent complete = new DispatchCompleteEvent();
      complete.setContext(context);
      complete.setSource(event);
      dispatch.fire(complete);
      return toReturn;
    }

    ReceiverStackInvocation.State state = new ReceiverStackInvocation.State(targets.size());

    for (ReceiverTarget target : targets) {
      ReceiverStackInvocation invocation = invocations.get();
      invocation.setContext(context);
      invocation.setEvent(event);
      invocation.setReceiverTarget(target);
      invocation.setState(state);
      toReturn.add(invocation);
    }

    return toReturn;
  }

  public int getPendingCount() {
    return pendingInvocations.get();
  }

  public boolean isDraining() {
    return isDraining.get();
  }

  public void setDraining(boolean drain) {
    pendingLock.lock();
    try {
      if (drain) {
        isDraining.set(true);
        // Don't block if called from a receiver, since this would block indefinitely
        if (receiverScope.inReceiver()) {
          return;
        }
        while (pendingInvocations.get() > 0) {
          pendingLockCondition.awaitUninterruptibly();
        }
      } else {
        isDraining.set(false);
      }
    } finally {
      pendingLock.unlock();
    }
  }

  void markComplete(ReceiverStackInvocation invocation) {
    pendingLock.lock();
    try {
      if (pendingInvocations.decrementAndGet() < 0) {
        // Should never happen
        pendingInvocations.set(0);
        throw new IllegalStateException("Too many drains");
      }
      pendingLockCondition.signalAll();
    } finally {
      pendingLock.unlock();
    }
  }
}
