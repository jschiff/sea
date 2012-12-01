package com.getperka.sea.jms.impl;

/*
 * #%L
 * Simple Event Architecture - JMS Support
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.getperka.sea.Event;

/**
 * Manages a mapping between {@link Event} and {@link EventMetadata} instances.
 */
@Singleton
public class EventMetadataMap {
  private final Lock cleanupLock = new ReentrantLock();
  private final ConcurrentMap<EventReference, EventMetadata> map =
      new ConcurrentHashMap<EventReference, EventMetadata>();
  private Provider<EventMetadata> metadata;
  private final ReferenceQueue<Event> staleEventReferences = new ReferenceQueue<Event>();

  protected EventMetadataMap() {}

  public EventMetadata get(Event event) {
    cleanup();
    EventMetadata toReturn = map.get(new EventReference(event, null));
    if (toReturn != null) {
      return toReturn;
    }
    toReturn = metadata.get();
    EventMetadata existing = map.putIfAbsent(new EventReference(event, staleEventReferences),
        toReturn);
    return existing == null ? toReturn : existing;
  }

  @Inject
  void inject(Provider<EventMetadata> metadata) {
    this.metadata = metadata;
  }

  private void cleanup() {
    if (cleanupLock.tryLock()) {
      try {
        Reference<? extends Event> ref;
        for (ref = staleEventReferences.poll(); ref != null; ref = staleEventReferences.poll()) {
          map.remove(ref);
        }
      } finally {
        cleanupLock.unlock();
      }
    }
  }
}