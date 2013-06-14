package com.getperka.sea.summary;

/*
 * #%L
 * Simple Event Architecture - Bits of Independently Reusable Decoration
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import com.getperka.sea.Event;

/**
 * Suppresses certain decorators from a {@link ReceiverSummary}. This can be used for decorators
 * that are global in nature or are otherwise uninteresting from a documentation perspective.
 * <p>
 * The following example will suprress the {@code Foo}, {@code Bar}, and {@code Baz} annotations.
 * 
 * <pre>
 * class MyConfigurationSummary extends ConfigurationSummary {
 *   &#064;Foo
 *   &#064;Bar
 *   &#064;Baz
 *   static class MyIgnore extends IgnoreDecorators {}
 * }
 * </pre>
 */
public class IgnoreDecorators extends DecorationTagger<Event> {

  @Override
  public List<String> describe(Class<? extends Event> event, AnnotatedElement elt) {
    return Collections.emptyList();
  }

  @Override
  public String getTagName() {
    return "Ignored";
  }

  @Override
  public boolean matches(Method method, Class<? extends Event> event, List<Annotation> annotations) {
    List<Class<?>> consumed = getConsumedAnnotations();
    for (Annotation a : annotations) {
      if (consumed.contains(a.annotationType())) {
        return true;
      }
    }
    return false;
  }
}