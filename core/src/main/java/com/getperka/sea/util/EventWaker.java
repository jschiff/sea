package com.getperka.sea.util;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.getperka.sea.ext.SuspendedEvent;

/**
 * A utility class for associating a {@link SuspendedEvent} with a key object that will later be
 * used to resume the event.
 * <p>
 * In addition to resuming events when a key is signaled, an EventWaker maintains a window of
 * recently-signaled keys to allow callers to detect a "just missed" condition.
 * <p>
 * EventWaker instances are thread-safe.
 */
public class EventWaker<T> {
  /**
   * Prevents concurrent mutation of {@link #registration} or the queues that it contains.
   */
  private final Lock lock = new ReentrantLock();
  private final ConcurrentMap<T, Long> recentSignals = new ConcurrentHashMap<T, Long>();
  private final AtomicLong recentWindowNanos = new AtomicLong();
  private final Map<T, Queue<SuspendedEvent>> registration = new HashMap<T, Queue<SuspendedEvent>>();
  private final ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();

  public EventWaker() {
    svc.schedule(new Runnable() {
      @Override
      public void run() {
        cleanup();
      }
    }, 1, TimeUnit.MINUTES);
  }

  /**
   * Reset all state in the EventWaker. Mainly intended for testing scenarios.
   */
  public void clear() {
    lock.lock();
    try {
      recentSignals.clear();
      registration.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns an approximate number of {@link SuspendedEvent} instances the {@code EventWaker} is
   * tracking for the given key.
   */
  public int getPendingEventCount(T key) {
    lock.lock();
    try {
      Queue<SuspendedEvent> queue = getQueue(key, false, false);
      return queue == null ? 0 : queue.size();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns {@code true} if {@code key} has been passed to {@link #signal} within the
   * {@link #setRecentWindow(long, TimeUnit) recent-key window}.
   * 
   * @param key a key value that may have been passed to {@link #signal}
   * @param remove if {@code true}, prevents any subsequent call to {@code isRecent} within the same
   *          timeout window from returning {@code true} unless the key is signaled again
   */
  public boolean isRecent(T key, boolean remove) {
    lock.lock();
    try {
      Long nanoTime = remove ? recentSignals.remove(key) : recentSignals.get(key);
      return nanoTime != null && (System.nanoTime() - nanoTime) < recentWindowNanos.get();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Register a {@link SuspendedEvent} to be resumed when {@code key} is passed to {@link #signal}
   * or the given time duration has elapsed. Multiple events may be associated with the same key;
   * all events registered to a key will be resumed when that key is signaled.
   * <p>
   * It is not an error if the event is resumed by means other than the EventWaker.
   * <p>
   * This method will return immediately.
   * 
   * @param key the value that will be used to resume the event
   * @param event a handle to the event to resume
   * @param delay the amount of time after which the event will be automatically resumed
   * @param unit the measurement unit of {@code delay}
   */
  public void resumeAfterSignal(final T key, final SuspendedEvent event, long delay, TimeUnit unit) {
    lock.lock();
    try {
      getQueue(key, true, false).add(event);
      svc.schedule(new Runnable() {
        @Override
        public void run() {
          scheduleResume(key, event);
        }
      }, delay, unit);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Sets an amount of time for which a key passed to {@link #signal} should be considered recent.
   * 
   * @param duration the amount of time to consider a key to be recent
   * @param unit the measurement unit of {@code duration}
   */
  public void setRecentWindow(long duration, TimeUnit unit) {
    if (duration <= 0) {
      recentWindowNanos.set(-1);
    } else {
      recentWindowNanos.set(unit.toNanos(duration));
    }
    cleanup();
  }

  /**
   * Trigger any {@link SuspendedEvent} instances waiting for {@code key}.
   */
  public void signal(T key) {
    lock.lock();
    try {
      recentSignals.put(key, System.nanoTime());
      Queue<SuspendedEvent> queue = getQueue(key, false, true);
      if (queue == null) {
        return;
      }
      for (SuspendedEvent evt : queue) {
        quietResume(evt);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Resumes all registered {@link SuspendedEvent} instances.
   */
  public void signalAll() {
    lock.lock();
    try {
      for (Queue<SuspendedEvent> queue : registration.values()) {
        for (SuspendedEvent evt : queue) {
          quietResume(evt);
        }
      }
      registration.clear();
    } finally {
      lock.unlock();
    }
  }

  void cleanup() {
    lock.lock();
    try {
      long window = recentWindowNanos.get();
      if (window <= 0) {
        recentSignals.clear();
        return;
      }
      long cutoff = System.nanoTime() - window;
      for (Iterator<Long> it = recentSignals.values().iterator(); it.hasNext();) {
        if (it.next() < cutoff) {
          it.remove();
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * For testing use only.
   */
  Map<T, Long> getRecentSignals() {
    return recentSignals;
  }

  private Queue<SuspendedEvent> getQueue(T key, boolean create, boolean remove) {
    Queue<SuspendedEvent> queue = remove ? registration.remove(key) : registration.get(key);
    if (queue == null && create) {
      queue = new ConcurrentLinkedQueue<SuspendedEvent>();
      registration.put(key, queue);
    }
    return queue;
  }

  private void quietResume(SuspendedEvent evt) {
    try {
      evt.resume();
    } catch (IllegalStateException ignored) {}
  }

  /**
   * Resume a {@link SuspendedEvent}, cleaning up {@link #registration} along the way.
   */
  private void scheduleResume(T key, final SuspendedEvent event) {
    lock.lock();
    try {
      Queue<SuspendedEvent> queue = getQueue(key, false, false);
      if (queue != null) {
        queue.remove(event);
        if (queue.isEmpty()) {
          getQueue(key, false, true);
        }
      }
    } finally {
      lock.unlock();
    }
    quietResume(event);
  }
}
