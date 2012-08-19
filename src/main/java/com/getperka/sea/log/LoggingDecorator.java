package com.getperka.sea.log;
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
import org.slf4j.LoggerFactory;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;

class LoggingDecorator implements EventDecorator<Logged, Event> {
  @Inject
  LoggingDecorator() {}

  @Override
  public Class<? extends Logged> getAnnotationType() {
    return Logged.class;
  }

  @Override
  public Class<? extends Event> getEventType() {
    return Event.class;
  }

  @Override
  public Callable<Object> wrap(final Context<Logged, Event> ctx) {
    return new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        Logged annotation = ctx.getAnnotation();
        Event event = ctx.getEvent();
        Logger logger = LoggerFactory.getLogger(event.getClass());
        logger.debug("Dispatching event");
        if (!annotation.info().isEmpty()) {
          logger.info(annotation.info());
        }
        if (!annotation.warn().isEmpty()) {
          logger.warn(annotation.warn());
        }
        if (!annotation.error().isEmpty()) {
          logger.error(annotation.error());
        }
        return ctx.getWork().call();
      }
    };
  }
}
