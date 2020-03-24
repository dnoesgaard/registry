package org.gbif.registry.persistence.mapper.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/** Mapper for {@link PipelineProcess} entities. */
public interface PipelineProcessMapper {

  /**
   * Inserts a new {@link PipelineProcess} if it doesn't exist. If it exists it handles the conflict
   * and doesn't break.
   *
   * <p>The id generated is set to the {@link PipelineProcess} received as parameter.
   *
   * @param process to insert
   */
  void createIfNotExists(PipelineProcess process);

  /**
   * Retrieves a {@link PipelineProcess} by dataset key and attempt.
   *
   * @param datasetKey
   * @param attempt
   * @return {@link PipelineProcess}
   */
  PipelineProcess getByDatasetAndAttempt(
      @Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt);

  /**
   * Retrieves a {@link PipelineProcess} by key.
   *
   * @param key key of the process
   * @return {@link PipelineProcess}
   */
  PipelineProcess get(@Param("key") long key);

  Optional<Integer> getLastAttempt(@Param("datasetKey") UUID datasetKey);

  /**
   * Adds a {@link PipelineExecution} to an existing {@link PipelineProcess}.
   *
   * @param pipelinesProcessKey key of the process where we want to add the step
   * @param execution execution to add
   */
  void addPipelineExecution(
      @Param("pipelinesProcessKey") long pipelinesProcessKey,
      @Param("execution") PipelineExecution execution);

  /**
   * Retrieves a {@link PipelineExecution} by key.
   *
   * @param pipelineExecutionKey key of the pipeline execution
   * @return {@link PipelineExecution}
   */
  PipelineExecution getPipelineExecution(@Param("key") long pipelineExecutionKey);

  /**
   * Adds a {@link PipelineStep} to an existing {@link PipelineExecution}.
   *
   * @param pipelineExecutionKey key of the process where we want to add the step
   * @param step step to add
   */
  void addPipelineStep(
      @Param("pipelineExecutionKey") long pipelineExecutionKey, @Param("step") PipelineStep step);

  /**
   * Lists {@link PipelineProcess} based in the search parameters.
   *
   * <p>It supports paging.
   *
   * @param datasetKey dataset key
   * @param attempt attempt
   * @param page page to specify the offset and the limit
   * @return list of {@link PipelineProcess}
   */
  List<PipelineProcess> list(
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("attempt") Integer attempt,
      @Nullable @Param("page") Pageable page);

  /** Counts the number of {@link PipelineProcess} based in the search parameters. */
  long count(
      @Nullable @Param("datasetKey") UUID datasetKey, @Nullable @Param("attempt") Integer attempt);

  PipelineStep getPipelineStep(@Param("key") long key);

  void updatePipelineStep(@Param("step") PipelineStep step);

  List<PipelineProcess> getPipelineProcessesByDatasetAndAttempts(
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("attempts") List<Integer> attempts);
}