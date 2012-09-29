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
 * A runtime tag that can be applied to an event.
 * <p>
 * Tag equality is based on a marker value, either a {@link String} or a {@link Class}.
 * String-based tags are more convenient to use, but there exists the possibility of collision
 * between unrelated components if a common marker value is used.
 */
public final class Tag {
  /**
   * Create a tag using a class as the marker.
   */
  public static Tag create(Class<?> clazz) {
    return new Tag(clazz.hashCode(), ("Class: " + clazz.getName()).intern());
  }

  /**
   * Create a tag using a receiver instance as the marker. When combined with
   * {@link Tagged#receiverInstance()}, this allows an event receiver to only receive the events
   * that it sent.
   */
  public static Tag create(Object receiver) {
    return new Tag(System.identityHashCode(receiver), receiver);
  }

  /**
   * Create a tag using a string as the marker.
   */
  public static Tag create(String string) {
    return new Tag(string.hashCode(), ("String: " + string).intern());
  }

  private final int hashCode;
  private final Object value;

  private Tag(int hashCode, Object value) {
    this.value = value;
    this.hashCode = value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Tag)) {
      return false;
    }
    /*
     * Object comparison intentional; string values will have been interned, and receivers must be
     * compared by identity, not by value.
     */
    return value == ((Tag) obj).value;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    // Avoid NPE with value.toString()
    return String.valueOf(value);
  }
}