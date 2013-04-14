package com.getperka.sea.inject;

/*
 * #%L
 * Simple Event Architecture - Core
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Test;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.TestConstants;
import com.getperka.sea.impl.HasInjector;
import com.google.inject.ProvisionException;

/**
 * Verify that placing an {@link ReceiverScoped} annotation on a type results in single instance per
 * event dispatch.
 */
public class ReceiverScopeTest {

  @ReceiverScoped
  static class IsEventScoped {}

  static class MyReceiver {
    @Inject
    IsEventScoped scopedObject;
    @Inject
    IsEventScoped scopedObject2;

    @Inject
    OtherObject otherObject;
    @Inject
    OtherObject otherObject2;

    @Inject
    Provider<IsEventScoped> scopedProvider;

    @Receiver
    void receive(TestEvent evt) {
      assertNotNull(scopedObject);
      assertSame(scopedObject, scopedObject2);
      assertSame(scopedObject, scopedProvider.get());
      assertSame(scopedProvider.get(), scopedProvider.get());
      assertNotSame(otherObject, otherObject2);
      evt.latch.countDown();
    }
  }

  static class OtherObject {}

  static class TestEvent implements Event {
    CountDownLatch latch = new CountDownLatch(1);
  }

  @Test(timeout = TestConstants.testDelay)
  public void test() throws InterruptedException {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(MyReceiver.class);

    TestEvent evt = new TestEvent();
    dispatch.fire(evt);
    evt.latch.await();

    // Try some other incorrect uses
    try {
      ((HasInjector) dispatch).getInstance(IsEventScoped.class);
      fail();
    } catch (ProvisionException expected) {}
    try {
      ((HasInjector) dispatch).getProvider(IsEventScoped.class).get();
      fail();
    } catch (ProvisionException expected) {}
  }
}
