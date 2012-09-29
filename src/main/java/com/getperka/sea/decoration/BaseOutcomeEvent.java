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
 * A base class that can be used when implementing {@link OutcomeEvent}.
 */
public class BaseOutcomeEvent extends BaseTaggedEvent implements OutcomeEvent {
  private Throwable failure;
  private boolean success;

  @Override
  public Throwable getFailure() {
    return failure;
  }

  @Override
  public boolean isSuccess() {
    return success;
  }

  @Override
  public void setFailure(Throwable failure) {
    this.failure = failure;
  }

  @Override
  public void setSuccess(boolean success) {
    this.success = success;
  }
}