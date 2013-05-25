package com.getperka.sea.sequence;

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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Registration;
import com.getperka.sea.decoration.Tag;

/**
 * A Sequencer manages a unit of work that must maintain state across a number of events. A sequence
 * succeeds or fails as a single unit, represented by a blocking call to {@link #call()}.
 * <p>
 * This class might also be named {@code Activity}, {@code Behavior}, or {@code Controller}, were
 * these names not excessively generic.
 * <p>
 * Users must call {@link #setEventDispatch(EventDispatch)} directly if not using an injection
 * framework to construct their Sequencer instances.
 * <p>
 * If concurrent instances of a Sequencer are expected, consider overriding {@link #fire(Event)} to
 * add an instance tag using {@link Tag#create(Object)} and annotate the subtype with
 * {@code @Tagged(receiverInstance = true)}.
 * 
 * @param <T> the type of data returned by the Sequencer
 */
public abstract class Sequencer<T> implements Callable<T> {
  /**
   * Ensures that only one thread at a time may call {@link #execute}.
   */
  private final Semaphore callSemaphore = new Semaphore(1, true);
  private EventDispatch dispatch;
  private final Condition done;
  /**
   * Controls access to general state, including {@link #isExecuting}.
   */
  private final ReentrantLock executingLock;
  private boolean isExecuting;
  private T toReturn;
  private SequenceFailureException toThrow;

  protected Sequencer() {
    executingLock = new ReentrantLock();
    done = executingLock.newCondition();
  }

  /**
   * Executes a sequence of events and blocks until one of {@link #finish} or {@link #fail} are
   * called.
   * <p>
   * Upon execution, the Sequencer instance will be automatically registered with the
   * {@link EventDispatch} instance. Similarly, the Sequencer will be detached from the
   * EventDispatch once a termination method has been called.
   * <p>
   * If one of the termination methods is not called, the method call will never return. Guaranteed
   * timeout behavior can be implemented by using a {@link ScheduledExecutorService} to schedule a
   * work unit to check {@link #isExecuting()} and possibly call {@link #fail} at a specific time.
   * 
   * @return the value provided to {@link #finish(Object)}.
   * @throws SequenceFailureException with the details of any call to {@link #fail}
   */
  @Override
  public T call() throws SequenceFailureException {
    if (dispatch == null) {
      throw new IllegalStateException("Must call setEventDispatch() before executing");
    }
    Registration registration = null;
    /*
     * Acquire the semaphore for this method. It ensures that only one thread at a time can enter
     * this method or wait for a specific call to finish(). Also grab the lock which allows us to
     * manipulate the done condition.
     */
    callSemaphore.acquireUninterruptibly();
    executingLock.lock();
    try {
      isExecuting = true;
      // Register for events
      registration = dispatch.register(this);
      // Start the work
      start();
      // Wait until finish has been called
      while (isExecuting) {
        done.awaitUninterruptibly();
      }
      // Maybe throw a failure exception
      if (toThrow != null) {
        throw toThrow;
      }
      // Return the value
      return toReturn;
    } finally {
      // Release the lock on isExecuting, which will have been cleared by finish() / fail()
      executingLock.unlock();
      // Don't retain the value being returned
      toReturn = null;
      toThrow = null;
      // Stop receiving events
      if (registration != null) {
        registration.cancel();
      }
      // At last, allow new callers into call()
      callSemaphore.release();
    }
  }

  /**
   * Returns the {@link EventDispatch} in use by the Sequencer.
   * 
   * @see #fire(Event)
   */
  public EventDispatch getEventDispatch() {
    return dispatch;
  }

  /**
   * Returns {@code true} if a thread is blocked by a call to {@link #call()}.
   */
  public boolean isExecuting() {
    executingLock.lock();
    try {
      return isExecuting;
    } finally {
      executingLock.unlock();
    }
  }

  @Inject
  public void setEventDispatch(EventDispatch dispatch) {
    this.dispatch = dispatch;
  }

  protected void fail(String message) {
    fail(message, null);
  }

  protected void fail(String message, Throwable cause) {
    executingLock.lock();
    try {
      checkExecuting();
      isExecuting = false;
      toThrow = new SequenceFailureException(message, cause);
      toThrow.fillInStackTrace();
      done.signalAll();
    } finally {
      executingLock.unlock();
    }
  }

  protected void finish(T toReturn) {
    executingLock.lock();
    try {
      checkExecuting();
      isExecuting = false;
      this.toReturn = toReturn;
      done.signalAll();
    } finally {
      executingLock.unlock();
    }
  }

  /**
   * Fires an event only if the Sequencer is executing.
   */
  protected void fire(Event event) {
    if (isExecuting()) {
      getEventDispatch().fire(event);
    }
  }

  /**
   * Subclasses must implement this method and ensure that one of {@link #finish} or {@link #fail}
   * are called (generally from a separate thread in response to an event callback).
   */
  protected abstract void start();

  private void checkExecuting() {
    if (!isExecuting) {
      throw new IllegalStateException("The Sequencer is not currently executing");
    }
  }
}
