package com.getperka.sea.decoration;
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

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.util.WeakEventReference;

@Singleton
class ExclusiveFilter implements EventDecorator<Exclusive, Event> {

  private final Lock cleanupLock = new ReentrantLock();
  private final ConcurrentMap<WeakEventReference<Event>, Lock> map =
      new ConcurrentHashMap<WeakEventReference<Event>, Lock>();
  private final ReferenceQueue<Event> queue = new ReferenceQueue<Event>();

  @Override
  public Callable<Object> wrap(final Context<Exclusive, Event> ctx) {
    return new Callable<Object>() {
      Lock lock = getLock(ctx.getEvent());

      @Override
      public Object call() throws Exception {
        lock.lock();
        try {
          return ctx.getWork().call();
        } finally {
          lock.unlock();
        }
      }
    };
  }

  Lock getLockForTesting(Event evt) {
    return map.get(new WeakEventReference<Event>(evt));
  }

  private Lock getLock(Event evt) {
    if (cleanupLock.tryLock()) {
      try {
        for (Object ref = queue.poll(); ref != null; ref = queue.poll()) {
          map.remove(ref);
        }
      } finally {
        cleanupLock.unlock();
      }
    }

    WeakEventReference<Event> ref = new WeakEventReference<Event>(evt, queue);
    Lock lock = map.get(ref);
    if (lock == null) {
      lock = new ReentrantLock();
      Lock temp = map.putIfAbsent(ref, lock);
      lock = temp == null ? lock : temp;
    }
    return lock;
  }
}
