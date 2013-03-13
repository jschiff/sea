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

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.EventContext;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.ReceiverScoped;

/**
 * Provides information about the disposition of a call to
 * {@link ReceiverTarget#dispatch(com.getperka.sea.Event)}.
 */
@ReceiverScoped
public class DispatchResultImpl implements DispatchResult {
  @CurrentEvent
  @Inject
  Event event;

  @Inject
  ReceiverMethodInvocation invocation;

  @Inject
  EventContext context;

  @Inject
  ReceiverTarget target;

  protected DispatchResultImpl() {}

  @Override
  public Event getEvent() {
    return event;
  }

  @Override
  public Object getReturnValue() {
    return invocation.getWasReturned();
  }

  @Override
  public ReceiverTarget getTarget() {
    return target;
  }

  @Override
  public Throwable getThrown() {
    return invocation.getWasThrown();
  }

  @Override
  public boolean wasReceived() {
    return invocation.getWasDispatched();
  }
}
