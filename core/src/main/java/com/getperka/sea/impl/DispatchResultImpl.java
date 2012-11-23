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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.ext.DispatchResult;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.DecoratorScoped;
import com.getperka.sea.inject.WasDispatched;
import com.getperka.sea.inject.WasReturned;
import com.getperka.sea.inject.WasThrown;

/**
 * Provides information about the disposition of a call to
 * {@link ReceiverTarget#dispatch(com.getperka.sea.Event)}.
 */
@DecoratorScoped
public class DispatchResultImpl implements DispatchResult {
  private Event event;
  private boolean received;
  private ReceiverTarget target;
  private Throwable thrown;
  private Object value;

  protected DispatchResultImpl() {}

  @Override
  public Event getEvent() {
    return event;
  }

  @Override
  public Object getReturnValue() {
    return value;
  }

  @Override
  public ReceiverTarget getTarget() {
    return target;
  }

  @Override
  public Throwable getThrown() {
    return thrown;
  }

  @Override
  public boolean wasReceived() {
    return received;
  }

  @Inject
  void inject(@CurrentEvent Event event, @WasDispatched AtomicBoolean received,
      ReceiverTarget target, @WasThrown AtomicReference<Throwable> thrown,
      @WasReturned AtomicReference<Object> value) {
    this.event = event;
    this.received = received.get();
    this.target = target;
    this.thrown = thrown.get();
    this.value = value.get();
  }
}
