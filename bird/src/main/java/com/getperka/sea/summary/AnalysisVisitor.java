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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.getperka.sea.Event;
import com.getperka.sea.ext.ConfigurationVisitor;

/**
 * Implementation class for {@link ConfigurationAnalyzer}.
 */
public class AnalysisVisitor extends ConfigurationVisitor {
  private final List<ReceiverSummary> receivers = new ArrayList<ReceiverSummary>();
  private final List<DecorationTagger<?>> taggers;

  AnalysisVisitor(List<DecorationTagger<?>> taggers) {
    this.taggers = taggers;
  }

  public List<ReceiverSummary> getReceivers() {
    return receivers;
  }

  @Override
  public void receiverMethod(Method method, Class<? extends Event> event,
      final List<Annotation> annotations) {

    // Find all taggers that match the receiver
    List<DecorationTagger<?>> matches = new ArrayList<DecorationTagger<?>>();
    for (DecorationTagger<?> when : taggers) {
      if (when.matches(method, event, annotations)) {
        matches.add(when);
      }
    }

    // Now create a map of the annotations and a facade to access them
    final Map<Class<?>, Annotation> map = new HashMap<Class<?>, Annotation>();
    for (Annotation a : annotations) {
      map.put(a.annotationType(), a);
    }

    // Provide access to the annotations
    AnnotatedElement elt = new AnnotatedElement() {
      @Override
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return annotationClass.cast(map.get(annotationClass));
      }

      @Override
      public Annotation[] getAnnotations() {
        return annotations.toArray(new Annotation[annotations.size()]);
      }

      @Override
      public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
      }

      @Override
      public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return map.containsKey(annotationClass);
      }
    };

    List<RenderedDescription> descriptions = new ArrayList<RenderedDescription>();
    descriptions.add(new RenderedDescription("Event", event.getSimpleName()));
    Set<Class<?>> remainingAnnotationType = new HashSet<Class<?>>(map.keySet());

    for (DecorationTagger<?> when : matches) {
      // Look for all / partial matches on remaining annotations
      List<Class<?>> consumed = when.getConsumedAnnotations();
      if (when.shouldMatchAll()) {
        if (!remainingAnnotationType.containsAll(consumed)) {
          continue;
        }
      } else {
        boolean matchedAny = false;
        for (Class<?> clazz : consumed) {
          if (remainingAnnotationType.contains(clazz)) {
            matchedAny = true;
            break;
          }
        }
        if (!matchedAny) {
          continue;
        }
      }
      remainingAnnotationType.removeAll(consumed);

      // Ask for descriptions
      @SuppressWarnings("unchecked")
      DecorationTagger<Event> cast = (DecorationTagger<Event>) when;
      List<String> alternates = cast.describe(event, elt);
      if (!alternates.isEmpty()) {
        descriptions.add(new RenderedDescription(when.getTagName(), alternates));
      }
    }

    // Put the remaninig decorations in just using their annotations
    for (Class<?> remaining : remainingAnnotationType) {
      String tag = "@" + remaining.getSimpleName();
      String string = map.get(remaining).toString();
      String values = string.substring(string.indexOf('(') + 1, string.length() - 1);
      if (values.isEmpty()) {
        values = tag;
      }
      descriptions.add(new RenderedDescription(tag, values));
    }

    String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
    List<ReceiverSummary> flattened = flatten(descriptions);
    for (ReceiverSummary summary : flattened) {
      summary.setMethodName(methodName);
      receivers.add(summary);
    }
  }

  private void flatten(List<ReceiverSummary> accumulator, List<RenderedDescription> prefix,
      List<RenderedDescription> remaining) {
    if (remaining.isEmpty()) {
      Map<String, String> map = new TreeMap<String, String>();
      for (RenderedDescription desc : prefix) {
        map.put(desc.getTag(), desc.iterator().next());
      }
      ReceiverSummary summary = new ReceiverSummary();
      summary.setTags(map);
      accumulator.add(summary);
      return;
    }

    RenderedDescription first = remaining.get(0);
    for (String alternate : first) {
      List<RenderedDescription> newPrefix = new ArrayList<RenderedDescription>(prefix);
      newPrefix.add(new RenderedDescription(first.getTag(), alternate));
      flatten(accumulator, newPrefix, remaining.subList(1, remaining.size()));
    }
  }

  private List<ReceiverSummary> flatten(List<RenderedDescription> descriptions) {
    List<ReceiverSummary> toReturn = new ArrayList<ReceiverSummary>();
    flatten(toReturn, Collections.<RenderedDescription> emptyList(), descriptions);
    return toReturn;
  }
}