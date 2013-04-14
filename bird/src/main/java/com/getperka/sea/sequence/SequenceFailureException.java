package com.getperka.sea.sequence;

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
 * Throws from {@link Sequencer#call()} if {@link Sequencer#fail} has been called.
 */
public class SequenceFailureException extends RuntimeException {
  private static final long serialVersionUID = 5247065513152393065L;

  public SequenceFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
