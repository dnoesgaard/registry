/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.utils.matcher;

import org.gbif.api.model.common.DOI;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsDoi extends TypeSafeMatcher<String> {

  @Override
  protected boolean matchesSafely(String s) {
    if (DOI.isParsable(s)) {
      DOI doi = new DOI(s);
      return doi.getPrefix().equals(DOI.TEST_PREFIX);
    }

    return false;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("DOI");
  }
}