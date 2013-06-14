package com.getperka.sea.ext;
/*
 * #%L
 * Simple Event Architecture - Core
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

import com.getperka.sea.EventDispatch;

/**
 * A capability interface for {@link EventDispatch}, {@link EventDecorator}, and
 * {@link EventObserver} interfaces to receive a {@link ConfigurationVisitor}. Extension may provide
 * their own ConfigurationVisitor subtypes to provide additional, extension-specific information to
 * the visitor.
 */
public interface ConfigurationProvider {
  /**
   * Supply the {@link ConfigurationVisitor} with information.
   */
  void accept(ConfigurationVisitor visitor);
}
