package com.getperka.sea.decoration;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;

/**
 * Implements a simple timeout mechanism for preventing excessive thread wall-time.
 */
class TimedDecorator implements EventDecorator<Timed, Event> {
  /**
   * Pipe-hitter.
   */
  static class ThreadKiller extends Thread {
    private final long killMilliTime;
    private final Thread toKill;
    private final boolean stop;
    private volatile boolean canceled;

    ThreadKiller(Thread toKill, Timed timed) {
      setName("ThreadKiller for " + toKill.getName());
      killMilliTime = System.currentTimeMillis() + timed.unit().toMillis(timed.value());
      this.toKill = toKill;
      stop = timed.stop();
    }

    public void cancel() {
      canceled = true;
      this.interrupt();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void run() {
      while (!canceled) {
        long remaining = killMilliTime - System.currentTimeMillis();
        if (remaining > 0) {
          try {
            Thread.sleep(remaining);
          } catch (InterruptedException ignored) {}
        } else {
          break;
        }
      }
      if (!canceled && toKill.isAlive()) {
        if (stop) {
          TimeoutError error = new TimeoutError();
          error.setStackTrace(toKill.getStackTrace());
          toKill.stop(error);
        } else {
          toKill.interrupt();
        }
      }
    }
  }

  /**
   * The exception type that is injected into the terminated thread.
   */
  @SuppressWarnings("serial")
  static class TimeoutError extends Throwable {}

  @Override
  public Callable<Object> wrap(final Context<Timed, Event> ctx) {
    return new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        TimeoutError timeout = null;
        ThreadKiller killer = new ThreadKiller(Thread.currentThread(), ctx.getAnnotation());
        killer.start();
        try {
          return call(ctx.getWork());
        } catch (TimeoutError e) {
          /*
           * It's possible that the timeout error may have been pused into the stack while a
           * decorator's Callable code is running.
           */
          timeout = e;
          return null;
        } finally {
          killer.cancel();
          if (ctx.wasThrown() instanceof TimeoutError) {
            timeout = (TimeoutError) ctx.wasThrown();
          }
          if (timeout != null) {
            Logger logger = LoggerFactory
                .getLogger(ctx.getTarget().getMethod().getDeclaringClass());
            logger.error("Timeout while executing " + ctx.getTarget(), ctx.wasThrown());
          }
        }
      }

      private Object call(Callable<Object> work) throws Exception, TimeoutError {
        return work.call();
      }
    };
  }
}
