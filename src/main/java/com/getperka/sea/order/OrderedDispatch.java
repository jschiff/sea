package com.getperka.sea.order;

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

import java.util.Collection;

import com.getperka.sea.Event;
import com.getperka.sea.EventDispatch;
import com.google.inject.ImplementedBy;

/**
 * Applies a strict order to a collection of events when dispatching to their receivers. The
 * receivers must be annotated with {@link Ordered}.
 * <p>
 * Instances of this class can be automatically injected into receiver instances created by
 * {@link EventDispatch}, or the {@link OrderedDispatchers#create(EventDispatch)} method may be
 * used.
 */
@ImplementedBy(OrderedDispatchImpl.class)
public interface OrderedDispatch {
  void fire(Collection<? extends Event> events);
}
