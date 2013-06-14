package com.getperka.sea.summary;
/*
 * #%L
 * Simple Event Architecture - Bits of Independently Reusable Decoration
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.EventDispatchers;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.BaseOutcomeEvent;
import com.getperka.sea.decoration.Failure;
import com.getperka.sea.decoration.Implementation;
import com.getperka.sea.decoration.Logged;
import com.getperka.sea.decoration.Success;

public class ConfigurationAnalyzerTest {

  static class MyEvent extends BaseOutcomeEvent {}

  static class MyReceiver {
    @Failure
    @Receiver
    void failure(MyEvent evt) {}

    @Implementation
    @Receiver
    @Logged
    void impl(MyEvent evt) {}

    @Receiver
    @Success
    void success(MyEvent evt) {}
  }

  @Test
  public void test() {
    EventDispatch dispatch = EventDispatchers.create();
    dispatch.register(MyReceiver.class);

    EventDispatchSummary summary = new BirdConfigurationAnalyzer().analyze(dispatch);

    assertEquals(3, summary.getReceivers().size());

    for (ReceiverSummary r : summary.getReceivers()) {
      String name = r.getMethodName();
      Map<String, String> tags = r.getTags();

      if ("MyReceiver.failure".equals(name)) {
        assertEquals("Failure", tags.get("Outcome"));
      } else if ("MyReceiver.impl".equals(name)) {
        assertEquals("Implementation", tags.get("Outcome"));
        assertEquals("level=DEBUG, value=, exceptionLevel=DEBUG", tags.get("@Logged"));
      } else if ("MyReceiver.success".equals(name)) {
        assertEquals("Success", tags.get("Outcome"));
      } else {
        fail(name);
      }
    }

    // Just verify it doesn't blow up
    assertNotNull(new SummaryUtils().createReceiverTsvReport(summary));
  }
}
