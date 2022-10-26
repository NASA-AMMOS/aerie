package gov.nasa.jpl.aerie.scheduler.server.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;

public record GenerateSchedulingLibAction(
    MissionModelService missionModelService
) {
  public GenerateSchedulingLibAction {
    Objects.requireNonNull(missionModelService);
  }

  /**
   * common interface for different possible results of the query
   */
  public sealed interface Response {
    record Failure(String reason) implements Response {}
    record Success(Map<String, String> files) implements Response {}
  }

  /**
   * execute the scheduling operation on the target plan (or retrieve existing scheduling results)
   *
   * @param missionModelId the id of the mission model for which to generate a scheduling library
   * @return a response object wrapping the results of generating the code (either successful or not)
   */
  public Response run(final MissionModelId missionModelId) {
    try {
      final var schedulingDslCompilerRoot = System.getenv("SCHEDULING_DSL_COMPILER_ROOT");
      final var schedulingDsl = Files.readString(Paths.get(schedulingDslCompilerRoot, "src", "libs", "scheduler-edsl-fluent-api.ts"));
      final var schedulerAst = Files.readString(Paths.get(schedulingDslCompilerRoot, "src", "libs", "scheduler-ast.ts"));
      final var windowsDsl = Files.readString(Paths.get(schedulingDslCompilerRoot, "src", "libs", "constraints", "constraints-edsl-fluent-api.ts"));
      final var windowsAst = Files.readString(Paths.get(schedulingDslCompilerRoot, "src", "libs", "constraints", "constraints-ast.ts"));
      final var temporalPolyfillTypes = Files.readString(Paths.get(schedulingDslCompilerRoot, "src", "libs", "TemporalPolyfillTypes.ts"));

      final var missionModelTypes = missionModelService.getMissionModelTypes(missionModelId);

      final var generatedSchedulerCode = TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(missionModelTypes);
      final var generatedConstraintsCode = gov.nasa.jpl.aerie.constraints.TypescriptCodeGenerationService
          .generateTypescriptTypes(
              ConstraintsTypescriptCodeGenerationHelper.activityTypes(missionModelTypes),
              ConstraintsTypescriptCodeGenerationHelper.resources(missionModelTypes));
      return new Response.Success(
          Map.of("file:///scheduler-edsl-fluent-api.ts", schedulingDsl,
                 "file:///scheduler-mission-model-generated-code.ts", generatedSchedulerCode,
                 "file:///scheduler-ast.ts", schedulerAst,
                 "file:///constraints-edsl-fluent-api.ts", windowsDsl,
                 "file:///constraints-ast.ts", windowsAst,
                 "file:///mission-model-generated-code.ts", generatedConstraintsCode,
                 "file:///TemporalPolyfillTypes.ts", temporalPolyfillTypes
                 ));
    } catch (NoSuchMissionModelException | IOException | MissionModelService.MissionModelServiceException e) {
      return new Response.Failure(e.getMessage());
    }
  }

}
