package com.getperka.sea.jms.impl;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.lang.ref.WeakReference;

import org.junit.Test;

import com.getperka.sea.Event;

public class EventReferenceTest {

  @Test
  public void testEquality() {
    Event event = new Event() {};
    Event event2 = new Event() {};

    EventReference refA = new EventReference(event, null);
    EventReference refB = new EventReference(event, null);
    EventReference ref2 = new EventReference(event2, null);

    checkEquals(refA, refA);
    checkEquals(refA, refB);

    assertFalse(refA.equals(ref2));
    assertFalse(refA.equals(null));
    assertFalse(refA.equals("Hello world!"));
  }

  @Test
  public void testGC() {
    Event event = new Event() {};
    WeakReference<Event> ref = new WeakReference<Event>(event);

    EventReference refA = new EventReference(event, null);
    EventReference refB = new EventReference(event, null);

    // See if we can get the garbage collector to play along
    assertNotNull(ref.get());
    event = null;
    System.gc();

    // May skip the rest of the test
    assumeTrue(ref.get() == null);

    checkEquals(refA, refA);
    assertFalse(refA.equals(refB));
  }

  @Test
  public void testNull() {
    try {
      new EventReference(null, null);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  private void checkEquals(EventReference a, EventReference b) {
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
  }
}
