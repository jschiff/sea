package com.getperka.sea.inject;

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

import com.getperka.sea.Event;
import com.getperka.sea.ext.ReceiverTarget;
import com.getperka.sea.impl.DispatchMap;
import com.getperka.sea.impl.Invocation;
import com.google.inject.Provider;

/**
 * Provides the {@link Invocation} instances that should be executed when dispatching the
 * {@link CurrentEvent}.
 */
@EventScoped
class InvocationProvider implements Provider<List<Invocation>> {

  private final Event event;
  private final Provider<Invocation> invocations;
  private final DispatchMap map;

  @Inject
  InvocationProvider(@CurrentEvent Event event, Provider<Invocation> invocations,
      DispatchMap map) {
    this.event = event;
    this.invocations = invocations;
    this.map = map;
  }

  @Override
  public List<Invocation> get() {
    Class<? extends Event> eventClass = event.getClass();
    List<Invocation> toReturn = new ArrayList<Invocation>();

    List<ReceiverTarget> targets = map.getTargets(eventClass);
    for (ReceiverTarget target : targets) {
      Invocation invocation = invocations.get();
      invocation.setInvocation(target);
      toReturn.add(invocation);
    }

    return toReturn;
  }
}
