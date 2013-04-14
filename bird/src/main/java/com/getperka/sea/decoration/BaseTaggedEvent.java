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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A base implementation of {@link TaggedEvent}.
 */
public class BaseTaggedEvent implements TaggedEvent {
  private final Set<Tag> tags = Collections.synchronizedSet(new HashSet<Tag>());

  /**
   * A convenience method for adding a tag to the event.
   */
  public void addTag(Tag tag) {
    tags.add(tag);
  }

  @Override
  public Set<Tag> getTags() {
    return tags;
  }
}