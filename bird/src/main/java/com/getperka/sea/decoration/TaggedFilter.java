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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import com.getperka.sea.ext.EventDecorator;

class TaggedFilter implements EventDecorator<Tagged, TaggedEvent> {
  @Override
  public Callable<Object> wrap(Context<Tagged, TaggedEvent> ctx) {
    Tagged annotation = ctx.getAnnotation();
    Set<Tag> annotationTags = new HashSet<Tag>();
    for (Class<?> clazz : annotation.classes()) {
      annotationTags.add(Tag.create(clazz));
    }
    for (String string : annotation.strings()) {
      annotationTags.add(Tag.create(string));
    }
    if (annotation.receiverInstance()) {
      annotationTags.add(Tag.create(ctx.getReceiverInstance()));
    }
    TaggedEvent evt = ctx.getEvent();

    final boolean ok;
    switch (annotation.mode()) {
      case ALL:
        ok = evt.getTags().containsAll(annotationTags);
        break;
      case ANY:
        ok = !Collections.disjoint(annotationTags, evt.getTags());
        break;
      case NONE:
        ok = Collections.disjoint(annotationTags, evt.getTags());
        break;
      default:
        throw new UnsupportedOperationException(annotation.mode().toString());
    }
    return ok ? ctx.getWork() : null;
  }
}