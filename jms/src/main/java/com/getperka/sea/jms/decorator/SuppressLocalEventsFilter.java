package com.getperka.sea.jms.decorator;

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

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.inject.EventLogger;
import com.getperka.sea.jms.impl.EventMetadataMap;
import com.getperka.sea.jms.impl.EventSubscriptionImpl;

/**
 * Implementation of {@link SuppressLocalEvents}.
 */
class SuppressLocalEventsFilter implements EventDecorator<SuppressLocalEvents, Event> {
  @EventLogger
  @Inject
  Logger logger;

  @Inject
  EventMetadataMap map;

  @Override
  public Callable<Object> wrap(Context<SuppressLocalEvents, Event> ctx) {
    // Always allow local events through to an EventSubscriber, or nothing will happen
    if (ctx.getReceiverInstance() instanceof EventSubscriptionImpl) {
      return ctx.getWork();
    }

    for (Class<? extends Event> clazz : ctx.getAnnotation().value()) {
      if (clazz.isInstance(ctx.getEvent())) {
        return ctx.getContext() instanceof EventSubscriptionImpl ? ctx.getWork() : null;
      }
    }
    return ctx.getWork();
  }
}
