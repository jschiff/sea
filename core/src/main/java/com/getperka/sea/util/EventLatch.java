package com.getperka.sea.util;

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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.Registration;
import com.getperka.sea.ext.DrainEvent;

/**
 * A latch that waits for a specific number of Events. In addition to simply counting down,
 * instances of this type also record the targeted events that they have received.
 * <p>
 * An instance of an EventCountDownLatch is automatically attached to an EventDispatch instance when
 * it is created and will detach itself once the requested number of events have been collected.
 * <p>
 * Subclasses must declare ore or more {@link Receiver} methods that call {@link #countDown(Event)}.
 * 
 * @see CountDownLatch
 */
public abstract class EventLatch<T extends Event> {
  /**
   * Creates a latch to collect the next {@code count} events assignable to the requested type from
   * the given EventDispatch.
   * 
   * @param dispatch the event source to examine
   * @param eventType the requested event type
   * @param count the number of events requested. If {@code count} is zero, the latch will not be
   *          attached to {@code dispatch} until {@link #reset(int)} is called
   * @return a new instance of EventCountDownLatch that has already been registered with the
   *         EventDispatch instance
   */
  public static <E extends Event> EventLatch<E> create(EventDispatch dispatch,
      final Class<E> eventType, int count) {
    return new EventLatch<E>(dispatch, count) {
      @Receiver
      void event(Event event) {
        if (eventType.isInstance(event)) {
          countDown(eventType.cast(event));
        }
      }
    };
  }

  /**
   * Creates a latch to collect the next {@code count} events from the given EventDispatch.
   * 
   * @param dispatch the event source to examine
   * @param count the number of events requested. If {@code count} is zero, the latch will not be
   *          attached to {@code dispatch} until {@link #reset(int)} is called
   * @return a new instance of EventCountDownLatch that has already been registered with the
   *         EventDispatch instance
   */
  public static EventLatch<Event> create(EventDispatch dispatch, int count) {
    return create(dispatch, Event.class, count);
  }

  private final AtomicInteger count = new AtomicInteger();
  private final Lock countingLock = new ReentrantLock();
  private final EventDispatch dispatch;
  private Registration eventRegistration;
  private final Condition finishedCollection = countingLock.newCondition();
  private Queue<T> received = new ConcurrentLinkedQueue<T>();

  protected EventLatch(EventDispatch dispatch, int count) {
    this.dispatch = dispatch;

    reset(count);
  }

  @Inject
  EventLatch(EventDispatch dispatch) {
    this(dispatch, 0);
  }

  /**
   * Wait until the requested number of events have been received. If the latch is not collecting
   * events, this method will return immediately.
   */
  public void await() throws InterruptedException {
    countingLock.lock();
    try {
      while (isCollecting()) {
        finishedCollection.await();
      }
    } finally {
      countingLock.unlock();
    }
  }

  /**
   * Wait until the requested number of events have been received or the specified delay has
   * occurred. If the latch is not collecting events, this method will return immediately.
   * 
   * @param duration the number of time units to wait
   * @param unit the length of {@code duration}
   */
  public void await(long duration, TimeUnit unit) throws InterruptedException {
    countingLock.lock();
    try {
      if (isCollecting()) {
        finishedCollection.await(duration, unit);
      }
    } finally {
      countingLock.unlock();
    }
  }

  /**
   * A convenience method to reset the latch to receive one event, fire an event, wait up to the
   * specified amount of time, and return the first matching event.
   * 
   * @param toFire the event to fire
   * @param duration the maximum number of time units to wait
   * @param unit the length of {@code duration}
   * @return the single received event or {@code null} if the time period elapsed or the thread was
   *         interrupted
   */
  public T awaitSingleEventAfter(Event toFire, long duration, TimeUnit unit) {
    reset(1);
    dispatch.fire(toFire);
    try {
      await(duration, unit);
      return received.poll();
    } catch (InterruptedException ignored) {
      // Don't care
    } finally {
      reset(0);
    }
    return null;
  }

  /**
   * Wait until the requested number of events have been received. If the latch is not collecting
   * events, this method will return immediately.
   */
  public void awaitUninterruptibly() {
    countingLock.lock();
    try {
      while (isCollecting()) {
        finishedCollection.awaitUninterruptibly();
      }
    } finally {
      countingLock.unlock();
    }
  }

  /**
   * Returns the queue that the latch is collecting events into. The queue can be drained while the
   * latch is still attached to the {@link EventDispatch}.
   */
  public Queue<T> getEventQueue() {
    countingLock.lock();
    try {
      return received;
    } finally {
      countingLock.unlock();
    }
  }

  /**
   * Reset the state of the latch. A new queue will be used to collect events so users must call
   * {@link #getEventQueue()} after invoking this method.
   * <p>
   * If the latch had previously detached itself from the the {@link EventDispatch}, it will
   * re-attach. If the latch is currently attached and {@code count} is zero, the latch will detach
   * itself.
   * 
   * @param count the number of events to collect
   */
  public void reset(int count) {
    if (dispatch.isDraining()) {
      count = 0;
    }
    countingLock.lock();
    try {
      int previousCount = this.count.getAndSet(count);

      if (previousCount == 0 && count > 0) {
        eventRegistration = dispatch.registerWeakly(this);
      } else if (count == 0) {
        finishedCollection.signalAll();
        if (eventRegistration != null) {
          eventRegistration.cancel();
        }
      }

      received = new ConcurrentLinkedQueue<T>();
    } finally {
      countingLock.unlock();
    }
  }

  /**
   * Receives events from the {@link EventDispatch}. Events that match the filter are passed to the
   * {@link #eventCollected} hook method.
   */
  protected void countDown(T event) {
    countingLock.lock();
    try {
      if (!isCollecting()) {
        return;
      }
      received.add(event);
      if (count.decrementAndGet() == 0) {
        eventRegistration.cancel();
        eventRegistration = null;
        finishedCollection.signalAll();
      }

    } finally {
      countingLock.unlock();
    }
  }

  /**
   * Returns the {@link EventDispatch} instance the latch was configured with.
   */
  protected EventDispatch getEventDispatch() {
    return dispatch;
  }

  @Receiver(synchronous = true)
  void drain(DrainEvent evt) {
    reset(0);
  }

  boolean isCollecting() {
    return count.get() > 0;
  }
}
