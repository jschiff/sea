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

import javax.inject.Inject;

import com.getperka.sea.Event;
import com.getperka.sea.Receiver;
import com.getperka.sea.decoration.Success;
import com.getperka.sea.impl.BindingMap;
import com.google.inject.TypeLiteral;

/**
 * Summarizes decorator annotations into one or more tagged values.
 * <p>
 * DecorationTagger subtypes are intended to be annotated with some combination of decorator
 * annotations which will be used when matching the tagger against a specific receiver method. The
 * following tagger would be executed when the {@link Success} decorator is present on a receiver
 * method.
 * 
 * <pre>
 * &#064;Success
 * class SuccessTagger extends DecorationTagger&lt;OutcomeEvent&gt; {
 *   public List&lt;String&gt; describe(Class&lt;? extends OutcomeEvent&gt; event, AnnotatedElement elt) {
 *     return Collections.singletonList(&quot;true&quot;);
 *   }
 * 
 *   public String getTag() {
 *     return &quot;onSuccess&quot;;
 *   }
 * }
 * </pre>
 * 
 * DecorationTaggers may also coalesce information from several decorator annotations. In the
 * following case, the tagger would only be applied if a receiver is affected by both of the
 * {@code @Foo} and {@code @Bar} decorators. The {@link #shouldMatchAll()} method can be overridden
 * to allow only partial matches.
 * 
 * <pre>
 * &#064;Foo
 * &#064;Bar
 * class FooBarTagger extends DecorationTagger&lt;Event&gt; {}
 * </pre>
 * 
 * If a decorator annotation has parameters, they will be taken into account unless
 * {@link #shouldCompareAnnotations()} return {@code false}.
 * 
 * @param <E> the base type of event to consider
 */
public abstract class DecorationTagger<E extends Event> implements Comparable<DecorationTagger<?>> {
  private Map<Class<?>, Annotation> annotationMap;
  private Class<?> target;

  /**
   * Requires injection.
   */
  protected DecorationTagger() {}

  /**
   * Sorted by {@code E} assignability and then number of expected annotations.
   */
  @Override
  public final int compareTo(DecorationTagger<?> o) {
    if (target.equals(o.target)) {
      // Fall through
    } else if (target.isAssignableFrom(o.target)) {
      return -1;
    } else if (o.target.isAssignableFrom(target)) {
      return 1;
    }

    return annotationMap.size() - o.annotationMap.size();
  }

  /**
   * Analyze the decorator annotations applied to a {@link Receiver}.
   * 
   * @param event the type of event that the receiver operates on
   * @param elt Provides access to the
   * @return a list of mutually-exclusive tag values describing the decoration
   */
  public abstract List<String> describe(Class<? extends E> event, AnnotatedElement elt);

  /**
   * Returns the annotations that the tagger operates on.
   */
  public List<Class<?>> getConsumedAnnotations() {
    return new ArrayList<Class<?>>(annotationMap.keySet());
  }

  /**
   * The name of the tag to apply to the {@link ReceiverSummary}.
   */
  public abstract String getTagName();

  /**
   * Determines if the tagger should be applied to the given receiver method.
   * 
   * @param method the {@link Receiver} method
   * @param event the type of event that the receiver operates on
   * @param annotations all decorator annotations that apply to the receiver
   * @return {@code true} if the tagger should be considered
   */
  public boolean matches(Method method, Class<? extends Event> event, List<Annotation> annotations) {
    if (annotationMap.isEmpty()) {
      return false;
    }
    if (!target.isAssignableFrom(event)) {
      return false;
    }

    Set<Class<?>> remainingAnnotationTypes = new HashSet<Class<?>>(annotationMap.keySet());
    for (Annotation a : annotations) {
      if (remainingAnnotationTypes.remove(a.annotationType()) && shouldCompareAnnotations()) {
        Annotation compareTo = annotationMap.get(a.annotationType());
        if (!a.equals(compareTo)) {
          return false;
        }
      }
    }

    return shouldMatchAll() ? remainingAnnotationTypes.isEmpty() :
        remainingAnnotationTypes.size() < annotationMap.size();
  }

  /**
   * Indicates if annotation values should be used when matching.
   * 
   * @return {@code true}
   */
  protected boolean shouldCompareAnnotations() {
    return true;
  }

  /**
   * Indicates if all decorator annotations specified on the tagger must be present to match.
   * 
   * @return {@code true}
   */
  protected boolean shouldMatchAll() {
    return true;
  }

  @Inject
  void inject(TypeLiteral<E> typeLit, BindingMap bindings) {
    target = typeLit.getRawType();

    Map<Class<?>, Annotation> temp = new HashMap<Class<?>, Annotation>();
    for (Annotation a : getClass().getAnnotations()) {
      if (bindings.getDecorator(a) == null) {
        continue;
      }
      temp.put(a.annotationType(), a);
    }

    annotationMap = Collections.unmodifiableMap(temp);
  }
}