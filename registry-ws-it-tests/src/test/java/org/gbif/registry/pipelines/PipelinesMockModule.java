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
package org.gbif.registry.pipelines;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PipelinesMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ExecutorService.class).toInstance(Executors.newSingleThreadExecutor());
    bind(PipelinesHistoryTrackingService.class)
        .to(DefaultPipelinesHistoryTrackingService.class)
        .in(Scopes.SINGLETON);
    bind(IngestionHistoryService.class)
        .to(DefaultIngestionHistoryService.class)
        .in(Scopes.SINGLETON);
  }
}