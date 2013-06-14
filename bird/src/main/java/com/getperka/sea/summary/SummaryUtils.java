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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods.
 */
public class SummaryUtils {
  /**
   * Creates a simple report of the receivers and their annotations using a tab-separated-value
   * format.
   */
  public String createReceiverTsvReport(EventDispatchSummary summary) {
    List<ReceiverSummary> receivers = summary.getReceivers();
    Set<String> allTags = new HashSet<String>();
    for (ReceiverSummary s : receivers) {
      for (String tag : s.getTags().keySet()) {
        allTags.add(tag);
      }
    }
    List<String> orderedTags = new ArrayList<String>(allTags);
    Collections.sort(orderedTags);

    Formatter f = new Formatter();
    for (String tag : orderedTags) {
      f.format("%s\t", tag);
    }
    f.format("Method\n");

    for (ReceiverSummary s : receivers) {
      for (String tag : orderedTags) {
        String value = s.getTags().get(tag);
        f.format("%s\t", value == null ? "" : value);
      }
      f.format("%s\n", s.getMethodName());
    }

    try {
      return f.toString();
    } finally {
      f.close();
    }
  }
}
