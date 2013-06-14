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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Internal datastructure for computing the product of all tag values.
 */
class RenderedDescription implements Iterable<String> {
  private final List<String> alternates;
  private final String tag;

  public RenderedDescription(String tag, List<String> alternates) {
    this.alternates = alternates;
    this.tag = tag;
  }

  public RenderedDescription(String tag, String value) {
    alternates = Collections.singletonList(value);
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  @Override
  public Iterator<String> iterator() {
    return alternates.iterator();
  }

  @Override
  public String toString() {
    return alternates.toString();
  }
}