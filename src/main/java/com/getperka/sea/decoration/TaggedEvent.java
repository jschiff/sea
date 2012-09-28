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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import com.getperka.sea.Event;
import com.getperka.sea.ext.EventDecorator;
import com.getperka.sea.ext.EventDecoratorBinding;

/**
 * An event type that can be filtered by receiver based on runtime tags.
 */
public interface TaggedEvent extends Event {
  /**
   * A base implementation of {@link TaggedEvent}.
   */
  public class Base implements TaggedEvent {
    private final Set<Tag> tags = new HashSet<Tag>();

    public void addTag(Tag tag) {
      tags.add(tag);
    }

    @Override
    public Set<Tag> getTags() {
      return tags;
    }
  }

  /**
   * A runtime tag that can be applied to an event.
   * <p>
   * Tag equality is based on a marker value, either a {@link String} or a {@link Class}.
   * String-based tags are more convenient to use, but there exists the possibility of collision
   * between unrelated components if a common marker value is used.
   */
  public final class Tag {
    /**
     * Create a tag using a class as the marker.
     */
    public static Tag create(Class<?> clazz) {
      return new Tag(clazz.hashCode(), ("Class: " + clazz.getName()).intern());
    }

    /**
     * Create a tag using a receiver instance as the marker. When combined with
     * {@link Tagged#receiverInstance()}, this allows an event receiver to only receive the events
     * that it sent.
     */
    public static Tag create(Object receiver) {
      return new Tag(System.identityHashCode(receiver), receiver);
    }

    /**
     * Create a tag using a string as the marker.
     */
    public static Tag create(String string) {
      return new Tag(string.hashCode(), ("String: " + string).intern());
    }

    private final int hashCode;
    private final Object value;

    private Tag(int hashCode, Object value) {
      this.value = value;
      this.hashCode = value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Tag)) {
        return false;
      }
      /*
       * Object comparison intentional; string values will have been interned, and receivers must be
       * compared by identity, not by value.
       */
      return value == ((Tag) obj).value;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      // Avoid NPE with value.toString()
      return String.valueOf(value);
    }
  }

  /**
   * A receiver that should receive a {@link TaggedEvent} only if its tags match the
   */
  @Documented
  @EventDecoratorBinding(TaggedFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
  public @interface Tagged {
    /**
     * Match using class-based tags.
     */
    Class<?>[] classes() default {};

    /**
     * The matching mode to use. Defaults to {@link TagMode#ALL}.
     */
    TagMode mode() default TagMode.ALL;

    /**
     * Match using the instance of the receiver. Only events with a tag created using
     * {@link Tag#create(Object)} referencing the receiver instance will match. This matching mode
     * is especially useful when events are re-fired after being processed.
     * 
     * @see OutcomeEvent
     */
    boolean receiverInstance() default false;

    /**
     * Match using string-based tags.
     */
    String[] strings() default {};
  }

  static class TaggedFilter implements EventDecorator<Tagged, TaggedEvent> {
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

  /**
   * Alters how a {@link Tagged} annotation is compared to {@link TaggedEvent#getTags()}.
   */
  public enum TagMode {
    /**
     * The event matches if it has any of the tags described in the {@link Tagged} filter.
     */
    ANY,
    /**
     * The event matches only if it has all of the tags described in the {@link Tagged} filter.
     */
    ALL,
    /**
     * The event matches only if it has none of the tags described in the {@link Tagged} filter.
     */
    NONE;
  }

  /**
   * Returns a set containing the event's tags. The returned set may be immutable.
   */
  Set<Tag> getTags();
}
