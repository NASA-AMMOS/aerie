package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    record Success(String libraryCode) implements Response {}
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
      final var preface = Files.readString(Paths.get(schedulingDslCompilerRoot, "src", "libs", "scheduler-edsl-fluent-api.ts"));
      return new Response.Success(preface + "\n" + this.typescriptCodeGenerationService.generateTypescriptTypesForMissionModel(missionModelId));
    } catch (NoSuchMissionModelException | IOException e) {
      return new Response.Failure(e.getMessage());
    }
  }
}
