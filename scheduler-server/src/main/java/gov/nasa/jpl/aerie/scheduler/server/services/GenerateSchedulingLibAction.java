package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

public record GenerateSchedulingLibAction(TypescriptCodeGenerationService typescriptCodeGenerationService) {
  public GenerateSchedulingLibAction {
    Objects.requireNonNull(typescriptCodeGenerationService);
  }

  /**
   * common interface for different possible results of the query
   */
  public sealed interface Response {
    record Failure(String reason) implements Response {}
    record Success(String typescript, Map<String, String> files) implements Response {}
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
      final var generated = this.typescriptCodeGenerationService.generateTypescriptTypesForMissionModel(missionModelId);
      return new Response.Success(
          schedulingDsl + "\n" + generated, // TODO deprecate this when the UI is updated.
          Map.of("scheduling-edsl-fluent-api.ts", schedulingDsl,
                 "mission-model-generated-code.ts", generated,
                 "scheduler-ast.ts", schedulerAst));
    } catch (NoSuchMissionModelException | IOException e) {
      return new Response.Failure(e.getMessage());
    }
  }
}
