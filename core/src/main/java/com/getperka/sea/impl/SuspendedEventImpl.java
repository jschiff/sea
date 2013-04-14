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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.getperka.sea.ext.SuspendedEvent;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.EventExecutor;
import com.getperka.sea.inject.ReceiverScoped;

/**
 * A trivial in-memory implementation of SuspendedEvent that recycles the
 * {@link ReceiverStackInvocation}. A more sophisticated implementation could be injected that
 * persists the {@link CurrentEvent} to a storage mechanism.
 */
@ReceiverScoped
public class SuspendedEventImpl implements SuspendedEvent {
  @Inject
  ReceiverStackInvocation invocation;
  @EventExecutor
  @Inject
  ExecutorService svc;
  private final AtomicBoolean hasResumed = new AtomicBoolean();

  /**
   * Requires injection.
   */
  protected SuspendedEventImpl() {}

  @Override
  public void resume() {
    if (!hasResumed.compareAndSet(false, true)) {
      throw new IllegalStateException("Cannot resume a SuspendedEvent more than once");
    }

    if (invocation.isSynchronous()) {
      invocation.call();
    } else {
      svc.submit(invocation);
    }
  }
}
