package org.gbif.registry.ws.resources.pipelines;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.PipelineWorkflow;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;
import org.gbif.registry.pipelines.RunPipelineResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

/**
 * Pipelines History service.
 */
@RestController
@RequestMapping(value = "pipelines/history", produces = MediaType.APPLICATION_JSON_VALUE)
public class PipelinesHistoryResource {

  private static final String PROCESS_PATH = "process/";
  private static final String RUN_PATH = "run/";

  private final PipelinesHistoryTrackingService historyTrackingService;

  public PipelinesHistoryResource(PipelinesHistoryTrackingService historyTrackingService) {
    this.historyTrackingService = historyTrackingService;
  }

  /**
   * Lists the history of all pipelines.
   */
  @GetMapping
  public PagingResponse<PipelineProcess> history(Pageable pageable) {
    return historyTrackingService.history(pageable);
  }

  /**
   * Lists teh history of a dataset.
   */
  @GetMapping("{datasetKey}")
  public PagingResponse<PipelineProcess> history(@PathVariable("datasetKey") UUID datasetKey, Pageable pageable) {
    return historyTrackingService.history(datasetKey, pageable);
  }

  /**
   * Gets the data of a {@link PipelineProcess}.
   */
  @GetMapping("{datasetKey}/{attempt}")
  public PipelineProcess getPipelineProcess(@PathVariable("datasetKey") UUID datasetKey,
                                            @PathVariable("attempt") int attempt) {
    return historyTrackingService.get(datasetKey, attempt);
  }

  @PostMapping(value = PROCESS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public long createPipelineProcess(@RequestBody PipelineProcessParameters params) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.create(params.getDatasetKey(), params.getAttempt(), authentication.getName());
  }

  /**
   * Adds a new pipeline step.
   */
  @PostMapping(value = PROCESS_PATH + "{processKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public long addPipelineStep(@PathVariable("processKey") long processKey,
                              @RequestBody PipelineStep pipelineStep) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.addPipelineStep(processKey, pipelineStep, authentication.getName());
  }

  @GetMapping(PROCESS_PATH + "{processKey}/step/{stepKey}")
  public PipelineStep getPipelineStep(@PathVariable("processKey") long processKey,
                                      @PathVariable("stepKey") long stepKey) {
    return historyTrackingService.getPipelineStep(stepKey);
  }

  /**
   * Updates the step status.
   */
  @PutMapping(value = PROCESS_PATH + "{processKey}/step/{stepKey}",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void updatePipelineStepStatusAndMetrics(@PathVariable("processKey") long processKey,
                                                 @PathVariable("stepKey") long stepKey,
                                                 PipelineStep.Status status) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    historyTrackingService.updatePipelineStepStatusAndMetrics(processKey, stepKey, status, authentication.getName());
  }

  @GetMapping("workflow/{datasetKey}/{attempt}")
  public PipelineWorkflow getPipelineWorkflow(@PathVariable("datasetKey") UUID datasetKey,
                                              @PathVariable("attempt") int attempt) {
    return historyTrackingService.getPipelineWorkflow(datasetKey, attempt);
  }

  /**
   * Runs the last attempt for all datasets.
   */
  @PostMapping(RUN_PATH)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<RunPipelineResponse> runAll(@RequestParam("steps") String steps,
                                                    @RequestParam("reason") String reason) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runLastAttempt(
                        parseSteps(steps), reason, authentication.getName())));
  }

  /**
   * Restart last failed pipelines step for a dataset.
   */
  @PostMapping(RUN_PATH + "{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<RunPipelineResponse> runPipelineAttempt(@PathVariable("datasetKey") UUID datasetKey,
                                                                @RequestParam("steps") String steps,
                                                                @RequestParam("reason") String reason) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runLastAttempt(
                        datasetKey,
                        parseSteps(steps),
                        reason,
                        authentication.getName(),
                        null)));
  }

  /**
   * Re-run a pipeline step.
   */
  @PostMapping(RUN_PATH + "{datasetKey}/{attempt}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<RunPipelineResponse> runPipelineAttempt(@PathVariable("datasetKey") UUID datasetKey,
                                                                @PathVariable("attempt") int attempt,
                                                                @RequestParam("steps") String steps,
                                                                @RequestParam("reason") String reason) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runPipelineAttempt(
                        datasetKey,
                        attempt,
                        parseSteps(steps),
                        reason,
                        authentication.getName(),
                        null)));
  }

  /**
   * Transforms a {@link RunPipelineResponse} into a {@link ResponseEntity}.
   */
  private static ResponseEntity<RunPipelineResponse> toHttpResponse(RunPipelineResponse runPipelineResponse) {
    if (runPipelineResponse.getResponseStatus() == RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(runPipelineResponse);
    } else if (runPipelineResponse.getResponseStatus() == RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(runPipelineResponse);
    } else if (runPipelineResponse.getResponseStatus() == RunPipelineResponse.ResponseStatus.ERROR) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(runPipelineResponse);
    }

    return ResponseEntity.ok(runPipelineResponse);
  }

  private Optional<ResponseEntity<RunPipelineResponse>> checkRunInputParams(String steps, String reason) {
    if (Strings.isNullOrEmpty(steps) || Strings.isNullOrEmpty(reason)) {
      return Optional.of(
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(
                  RunPipelineResponse.builder()
                      .setMessage("Steps and reason parameters are required")
                      .build()));
    }

    return Optional.empty();
  }

  /**
   * Parse steps argument.
   */
  private Set<StepType> parseSteps(String steps) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(steps));
    return Arrays.stream(steps.split(","))
        .map(s -> StepType.valueOf(s.toUpperCase()))
        .collect(Collectors.toSet());
  }
}
