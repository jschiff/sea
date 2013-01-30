package com.getperka.sea.jms.ext;

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.getperka.sea.jms.DestinationType;
import com.getperka.sea.jms.ReturnMode;
import com.getperka.sea.jms.RoutingMode;
import com.getperka.sea.jms.SubscriptionMode;
import com.getperka.sea.jms.SubscriptionOptions;

/**
 * A factory for creating and configuring instances of {@link SubscriptionOptions}.
 */
public class SubscriptionOptionsBuilder {

  static class Handler implements InvocationHandler {
    private final Map<String, Object> values;

    Handler(Map<String, Object> values) {
      this.values = values;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SubscriptionOptions)) {
        return false;
      }

      /*
       * If the implementation is backed by a Handler, compare the value maps directly. Otherwise,
       * use a new builder to extract the annotation values and then compare.
       */
      Map<String, Object> compareTo;
      if (Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof Handler) {
        Handler handler = (Handler) Proxy.getInvocationHandler(obj);
        // Quick exit for comparison to self
        if (this == handler) {
          return true;
        }
        compareTo = handler.peek();
      } else {
        compareTo = new SubscriptionOptionsBuilder()
            .copyFrom((SubscriptionOptions) obj)
            .peek();
      }
      return values.equals(compareTo);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }

    @Override
    public Object invoke(Object instance, Method m, Object[] args) throws Throwable {
      if (Object.class.equals(m.getDeclaringClass())) {
        return m.invoke(this, args);
      }
      Object toReturn = values.get(m.getName());
      if (toReturn == null) {
        return m.getDefaultValue();
      }
      // Add a cast so if there's bad data in the map, the CCE will occur someplace that makes sense
      return m.getReturnType().cast(toReturn);
    }

    @Override
    public String toString() {
      return values.toString();
    }

    /**
     * Visible for testing.
     */
    Map<String, Object> peek() {
      return values;
    }
  }

  private Map<String, Object> values = new HashMap<String, Object>();

  /**
   * Returns an immutable instance of SubscriptionOptions.
   * <p>
   * This method may be called multiple times. Each time it is called, a new
   * {@link SubscriptionOptions} instance will be returned. Subsequent mutations to the builder will
   * not affect previously-constructed instances.
   */
  public SubscriptionOptions build() {
    Map<String, Object> temp = new HashMap<String, Object>(values);
    return (SubscriptionOptions) Proxy.newProxyInstance(SubscriptionOptions.class.getClassLoader(),
        new Class<?>[] { SubscriptionOptions.class }, new Handler(temp));
  }

  public SubscriptionOptionsBuilder copyFrom(SubscriptionOptions template) {
    Throwable ex;
    try {
      for (Method m : SubscriptionOptions.class.getDeclaredMethods()) {
        values.put(m.getName(), m.invoke(template));
      }
      return this;
    } catch (InvocationTargetException e) {
      ex = e.getCause();
    } catch (IllegalArgumentException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    }
    throw new RuntimeException("Could not copy public methods", ex);
  }

  public SubscriptionOptionsBuilder destinationName(String name) {
    values.put("destinationName", name);
    return this;
  }

  public SubscriptionOptionsBuilder destinationType(DestinationType type) {
    values.put("destinationType", type);
    return this;
  }

  public SubscriptionOptionsBuilder durableSubscriberId(String name) {
    values.put("durableSubscriberId", name);
    return this;
  }

  public SubscriptionOptionsBuilder messageSelector(String selector) {
    values.put("messageSelector", selector);
    return this;
  }

  public SubscriptionOptionsBuilder returnMode(ReturnMode mode) {
    values.put("returnMode", mode);
    return this;
  }

  public SubscriptionOptionsBuilder routingMode(RoutingMode mode) {
    values.put("routingMode", mode);
    return this;
  }

  public SubscriptionOptionsBuilder subscriptionMode(SubscriptionMode mode) {
    values.put("subscriptionMode", mode);
    return this;
  }

  /**
   * Used by the handler and for testing.
   */
  Map<String, Object> peek() {
    return values;
  }
}
