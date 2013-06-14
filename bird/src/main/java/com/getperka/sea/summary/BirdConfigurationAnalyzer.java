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

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import com.getperka.sea.decoration.Failure;
import com.getperka.sea.decoration.Implementation;
import com.getperka.sea.decoration.OutcomeEvent;
import com.getperka.sea.decoration.Success;

/**
 * A subclass of {@link ConfigurationAnalyzer} with various presets for annotations in the
 * {@code sea-bird} module.
 */
public class BirdConfigurationAnalyzer extends ConfigurationAnalyzer {

  /**
   * Coalesces {@link Implementation}, {@link Failure}, and {@link Success} decorators into a single
   * {@code Outcome} tag.
   */
  @Implementation
  @Failure
  @Success
  public static class OutcomeTagger extends DecorationTagger<OutcomeEvent> {

    @Override
    public List<String> describe(Class<? extends OutcomeEvent> event, AnnotatedElement elt) {
      List<String> toReturn = new ArrayList<String>();

      if (elt.isAnnotationPresent(Implementation.class)) {
        toReturn.add("Implementation");
      }
      if (elt.isAnnotationPresent(Failure.class)) {
        toReturn.add("Failure");
      }
      if (elt.isAnnotationPresent(Success.class)) {
        toReturn.add("Success");
      }

      return toReturn;
    }

    @Override
    public String getTagName() {
      return "Outcome";
    }

    @Override
    protected boolean shouldCompareAnnotations() {
      return false;
    }

    @Override
    protected boolean shouldMatchAll() {
      return false;
    }
  }
}
