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

import java.util.Collection;

import com.getperka.sea.ext.EventDecorator;

/**
 * A CompositeEvent allows an event to expose multiple facets to be presented to an
 * {@link EventDecorator} chain without having to implement those interfaces directly.
 * <p>
 * For example, a concrete Event type that wants to provide compatibility with a
 * {@code FooDecorator} and {@code BarDecorator} could either choose to implement the associated
 * {@code FooEvent} and {@code BarEvent} interfaces or instead choose to return a {@code FooEvent}
 * and a {@code BarEvent} via {@link #getEventFacets()}. In addition to avoiding the unnecessary
 * composition-by-inheritance clutter, this allows EventDecorators to be written against concrete
 * types without requiring users to extend a specific base type.
 * <p>
 * When matching which object to pass to an {@link EventDecorator}, the decorator's parameterization
 * is examined. If the event being fired implements the requested interface, it will be passed in.
 * Otherwise, the facets will be examined for assignability in order, with the first matching facet
 * being used. If neither the base event nor any of its facets are assignable to the requested
 * interface, the decorator will be ignored.
 */
public interface CompositeEvent extends Event {
  /**
   * Returns the facets associated with the event.
   */
  Collection<? extends Event> getEventFacets();
}
