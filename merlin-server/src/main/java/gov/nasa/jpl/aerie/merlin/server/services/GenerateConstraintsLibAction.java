package gov.nasa.jpl.aerie.merlin.server.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

public record GenerateConstraintsLibAction(TypescriptCodeGenerationService typescriptCodeGenerationService) {
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
   *
   * @param missionModelId
   * @return a response object wrapping the results of generating the code (either successful or not)
   */
  public Response run(final String missionModelId) {

    try {
      final var constraintsDslCompilerRoot = System.getenv("CONSTRAINTS_DSL_COMPILER_ROOT");
      final var constraintsApi = Files.readString(Paths.get(constraintsDslCompilerRoot, "src", "libs", "constraints-edsl-fluent-api.ts"));
      final var constraintsAst = Files.readString(Paths.get(constraintsDslCompilerRoot, "src", "libs", "constraints-ast.ts"));
      final var generated = TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel();
      return new Response.Success(
          Map.of("constraints-edsl-fluent-api.ts", constraintsApi,
                 "mission-model-generated-code.ts", generated,
                 "constraints-ast.ts", constraintsAst));
    } catch (IOException e) {
      return new Response.Failure(e.getMessage());
    }
  }
}
