package com.getperka.sea.log;

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
