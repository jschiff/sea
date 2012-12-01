package com.getperka.sea.testing;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.Registration;

/**
 * A utility class that waits for a specific number of Events assignable to a particular type. In
 * addition to simply counting down, instances of this type also record the targeted events that
 * they have received.
 * 
 * @see CountDownLatch
 */
public class EventCountDownLatch<T extends Event> {
  public static <E extends Event> EventCountDownLatch<E> create(EventDispatch dispatch,
      Class<E> eventType, int count) {
    return new EventCountDownLatch<E>(dispatch, eventType, count);
  }

  public static EventCountDownLatch<Event> create(EventDispatch dispatch, int count) {
    return create(dispatch, Event.class, count);
  }

  private final EventDispatch dispatch;
  private Registration eventRegistration;
  private CountDownLatch latch;
  private final Lock countingLock = new ReentrantLock();
  private Queue<T> received = new ConcurrentLinkedQueue<T>();
  private Class<T> targetType;

  protected EventCountDownLatch(EventDispatch dispatch, Class<T> eventType, int count) {
    this.dispatch = dispatch;
    this.targetType = eventType;

    reset(count);
  }

  public void await() throws InterruptedException {
    latch.await();
  }

  public void await(long duration, TimeUnit unit) throws InterruptedException {
    latch.await(duration, unit);
  }

  /**
   * Calls {@link #countDown()} when an event is receiver that is assignable to the target event
   * type.
   */
  @Receiver
  public void countDown(Event event) {
    countingLock.lock();
    try {
      if (targetType.isInstance(event)) {
        received.add(targetType.cast(event));
        latch.countDown();
        if (latch.getCount() == 0) {
          eventRegistration.cancel();
        }
      }
    } finally {
      countingLock.unlock();
    }
  }

  public Queue<T> getEventQueue() {
    return received;
  }

  public Class<T> getTargetType() {
    return targetType;
  }

  public void reset(int count) {
    countingLock.lock();
    try {
      if (eventRegistration != null) {
        eventRegistration.cancel();
      }

      latch = new CountDownLatch(count);
      received = new ConcurrentLinkedQueue<T>();
      if (count > 0) {
        eventRegistration = dispatch.register(this);
      }
    } finally {
      countingLock.unlock();
    }
  }
}
