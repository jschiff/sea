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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.getperka.sea.EventDispatch;
import com.getperka.sea.Receiver;
import com.getperka.sea.impl.HasInjector;
import com.google.inject.Injector;

/**
 * Produces a textual summary of the current configuration of an {@link EventDispatch}.
 * <p>
 * The analyzer can be configured by creating a subclass that defines one or more static
 * {@link DecorationTagger} implementations. These taggers can be used to extract information from
 * receiver decorations in a more human-friendly format. The list of effective decoration
 * annotations will be computed for each {@link Receiver} method. Each known
 * {@link DecorationTagger} will have an opportunity to consume one or more of the decorations and
 * provide some number of tagged values that describe the receiver. Each decorator annotation may be
 * consumed exactly once.
 */
public class ConfigurationAnalyzer {

  public EventDispatchSummary analyze(EventDispatch dispatch) {
    Injector injector = ((HasInjector) dispatch).getInjector();

    final List<DecorationTagger<?>> whens = new ArrayList<DecorationTagger<?>>();
    for (Class<? extends DecorationTagger<?>> clazz : getTaggers()) {
      whens.add(injector.getInstance(clazz));
    }
    // Sort the matchers by greediest first
    Collections.sort(whens, Collections.reverseOrder());

    AnalysisVisitor visitor = new AnalysisVisitor(whens);
    dispatch.accept(visitor);

    EventDispatchSummary toReturn = new EventDispatchSummary();
    toReturn.setReceivers(visitor.getReceivers());

    return toReturn;
  }

  /**
   * Examines the {@link ConfigurationAnalyzer} class's supertype hierarchy for any static inner
   * classes that extend {@link DecorationTagger} or for a {@link DecorationTaggers} annotation.
   */
  protected List<Class<? extends DecorationTagger<?>>> getTaggers() {
    List<Class<? extends DecorationTagger<?>>> toReturn = new ArrayList<Class<? extends DecorationTagger<?>>>();

    Class<?> lookAt = getClass();
    do {
      for (Class<?> inner : lookAt.getDeclaredClasses()) {
        if (DecorationTagger.class.isAssignableFrom(inner)
          && Modifier.isStatic(inner.getModifiers())) {
          @SuppressWarnings("unchecked")
          Class<? extends DecorationTagger<?>> temp = (Class<? extends DecorationTagger<?>>) inner;
          toReturn.add(temp);
        }
      }

      DecorationTaggers taggers = lookAt.getAnnotation(DecorationTaggers.class);
      if (taggers != null) {
        toReturn.addAll(Arrays.asList(taggers.value()));
      }
      lookAt = lookAt.getSuperclass();
    } while (!Object.class.equals(lookAt));

    return toReturn;
  }

}
