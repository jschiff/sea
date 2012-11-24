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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.ext.DispatchCompleteEvent;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.inject.CurrentEvent;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.inject.EventScoped;
import com.google.inject.Provider;

/**
 * Provides the {@link Invocation} instances that should be executed when dispatching the
 * {@link CurrentEvent}.
 */
@EventScoped
public class InvocationProvider implements Provider<List<Invocation>> {

  private final EventDispatch dispatch;
  private final Event event;
  private final Provider<Invocation> invocations;
  private final Logger logger;
  private final DispatchMap map;

  @Inject
  InvocationProvider(EventDispatch dispatch, @CurrentEvent Event event,
      Provider<Invocation> invocations,
      @EventLogger Logger logger, DispatchMap map) {
    this.dispatch = dispatch;
    this.event = event;
    this.invocations = invocations;
    this.logger = logger;
    this.map = map;
  }

  @Override
  public List<Invocation> get() {
    Class<? extends Event> eventClass = event.getClass();
    List<ReceiverTarget> targets = map.getTargets(eventClass);
    List<Invocation> toReturn = new ArrayList<Invocation>();

    // Fire an empty DispatchComplete if there are no receivers
    if (targets.isEmpty() && !(event instanceof DispatchCompleteEvent)) {
      logger.debug("No @Receiver methods that accept {} have been registered",
          eventClass.getName());

      DispatchCompleteEvent complete = new DispatchCompleteEvent();
      complete.setSource(event);
      dispatch.fire(complete);
      return toReturn;
    }

    Invocation.State state = new Invocation.State(targets.size());

    for (ReceiverTarget target : targets) {
      Invocation invocation = invocations.get();
      invocation.setInvocation(target);
      invocation.setState(state);
      toReturn.add(invocation);
    }

    return toReturn;
  }
}