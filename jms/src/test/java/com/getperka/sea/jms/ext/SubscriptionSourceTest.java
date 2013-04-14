package com.getperka.sea.jms.ext;

/*
 * #%L
 * Simple Event Architecture - JMS Support
 * %%
 * Copyright (C) 2012 - 2013 Perka Inc.
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

import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.impl.HasInjector;
import com.getperka.sea.jms.JmsTestBase;
import com.getperka.sea.jms.SubscriptionOptions;
import com.getperka.sea.jms.Subscriptions;
import com.getperka.sea.jms.ext.SubscriptionSourceTest.MySource;
import com.getperka.sea.jms.impl.EventSubscriber;

/**
 * Verifies that {@link SubscriptionSource} registration works.
 */
@Subscriptions(sources = MySource.class)
public class SubscriptionSourceTest extends JmsTestBase {
  static class MyEvent implements Event, Serializable {
    private static final long serialVersionUID = 1L;
  }

  static class MySource implements SubscriptionSource {
    @Override
    public void configureSubscriptions(Context context) {
      context.subscribe(MyEvent.class, SubscriptionOptions.DEFAULT);
    }
  }

  @Test
  public void test() {
    eventDispatch.addGlobalDecorator(getClass());

    EventSubscriber subscriber = ((HasInjector) eventDispatch).getInstance(EventSubscriber.class);
    assertTrue(subscriber.isSubscribed(MyEvent.class));
  }
}
