package com.getperka.sea;
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

import com.getperka.sea.ext.ReceiverTarget;

/**
 * Indicates that a unsatisfiable {@code @Receiver} method declaration was encountered during
 * registration.
 */
public class BadReceiverException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final ReceiverTarget target;

  public BadReceiverException(String message, ReceiverTarget target, Exception cause) {
    super(message + " at " + target, cause);
    this.target = target;
  }

  public ReceiverTarget getTarget() {
    return target;
  }
}
