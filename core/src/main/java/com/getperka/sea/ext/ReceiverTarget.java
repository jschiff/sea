package com.getperka.sea.ext;

import com.getperka.sea.Receiver;

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
 * Encapsulates a method and an instance on which to execute it.
 * <p>
 * Two ReceiverTargets are equal only if they will dispatch to the same method on the same instance
 * (or no instance in the case of static receivers).
 */
public interface ReceiverTarget {
  /**
   * Returns the value of the {@link Receiver#synchronous()} property.
   */
  boolean isSynchronous();

  /**
   * For debugging use only. Returns the signature of the method that the ReceiverTarget will
   * execute.
   */
  @Override
  String toString();
}
