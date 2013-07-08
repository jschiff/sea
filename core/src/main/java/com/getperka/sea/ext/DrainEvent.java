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

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.getperka.sea.util.EventLatch;

/**
 * Fired when {@link EventDispatch#setDraining(boolean) EventDispatch.setDraining(true)} is called
 * to allow any receivers, such as an {@link EventLatch}, to short-circuit their waits, since no
 * additional events are likely to be seen.
 */
public class DrainEvent implements Event {
  public DrainEvent() {}
}
