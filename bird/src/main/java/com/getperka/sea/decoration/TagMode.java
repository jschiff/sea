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

/**
 * Alters how a {@link Tagged} annotation is compared to {@link TaggedEvent#getTags()}.
 */
public enum TagMode {
  /**
   * The event matches if it has any of the tags described in the {@link Tagged} filter.
   */
  ANY,
  /**
   * The event matches only if it has all of the tags described in the {@link Tagged} filter.
   */
  ALL,
  /**
   * The event matches only if it has none of the tags described in the {@link Tagged} filter.
   */
  NONE;
}