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

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.EventScoped;

/**
 * The top-level invocation of a {@link ReceiverTarget ReceiverTarget's} work. This class sets the
 * name of the current thread and provides unhandled exception dispatch for exceptions that occur
 * within the decorator / dispatch plumbing.
 */
@EventScoped
public class Invocation implements Callable<Object> {
  private Event event;
  private ReceiverTarget target;
  private Logger logger;

  protected Invocation() {}

  @Override
  public Object call() {
    Thread.currentThread().setName(toString());
    try {
      return target.dispatch(event);
    } catch (Exception e) {
      logger.error("Unhandled exception during event dispatch", e);
      return null;
    } finally {
      Thread.currentThread().setName("idle");
    }
  }

  public void setInvocation(ReceiverTarget target) {
    this.target = target;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return target.toString();
  }

  @Inject
  void inject(@CurrentEvent Event event, @EventLogger Logger logger) {
    this.event = event;
    this.logger = logger;
  }
}
