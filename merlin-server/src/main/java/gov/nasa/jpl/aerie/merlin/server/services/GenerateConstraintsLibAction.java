package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

public record GenerateConstraintsLibAction(ConstraintsCodeGenService typescriptCodeGenerationService) {
  public GenerateConstraintsLibAction {
    Objects.requireNonNull(typescriptCodeGenerationService);
  }

  /**
   * common interface for different possible results of the query
   */
  public sealed interface Response {
    record Failure(String reason) implements Response {}
    record Success(Map<String, String> files) implements Response {}
  }

  /**
   * Generate the source files used for running constraint definitions.
   * Do so for a given plan, meaning include external resource profiles in the definition!
   *
   * @param planId the plan id.
   * @return a response object wrapping the results of generating the code (either successful or not)
   */
  public Response runPlan(PlanId planId, PlanRepository planRepository) {

    try {
      final var constraintsDslCompilerRoot = System.getenv("CONSTRAINTS_DSL_COMPILER_ROOT");
      final var constraintsApi = Files.readString(Paths.get(constraintsDslCompilerRoot, "src", "libs", "constraints-edsl-fluent-api.ts"));
      final var constraintsAst = Files.readString(Paths.get(constraintsDslCompilerRoot, "src", "libs", "constraints-ast.ts"));
      final var generated = typescriptCodeGenerationService.generateTypescriptTypesFromPlan(planId, planRepository);
      return new Response.Success(
          Map.of("constraints-edsl-fluent-api.ts", constraintsApi,
                 "mission-model-generated-code.ts", generated,
                 "constraints-ast.ts", constraintsAst));
    } catch (MissionModelService.NoSuchMissionModelException | IOException | NoSuchPlanException e) {
      return new Response.Failure(e.getMessage());
    }
  }

  /**
   * Generate the source files used for running constraint definitions.
   * Do so for a given mission model, meaning do not include external resource profiles in the definition!
   *
   * @param missionModelId the mission model id.
   * @return a response object wrapping the results of generating the code (either successful or not)
   */
  public Response runModel(String missionModelId) {

    try {
      final var constraintsDslCompilerRoot = System.getenv("CONSTRAINTS_DSL_COMPILER_ROOT");
      final var constraintsApi = Files.readString(Paths.get(constraintsDslCompilerRoot, "src", "libs", "constraints-edsl-fluent-api.ts"));
      final var constraintsAst = Files.readString(Paths.get(constraintsDslCompilerRoot, "src", "libs", "constraints-ast.ts"));
      final var generated = typescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(missionModelId);
      return new Response.Success(
          Map.of("constraints-edsl-fluent-api.ts", constraintsApi,
                 "mission-model-generated-code.ts", generated,
                 "constraints-ast.ts", constraintsAst));
    } catch (MissionModelService.NoSuchMissionModelException | IOException e) {
      return new Response.Failure(e.getMessage());
    }
  }
}
