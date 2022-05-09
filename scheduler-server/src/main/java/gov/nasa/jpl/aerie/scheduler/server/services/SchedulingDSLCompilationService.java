package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingCompilationError;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;

public class SchedulingDSLCompilationService {

  private final Process nodeProcess;
  private final TypescriptCodeGenerationService typescriptCodeGenerationService;

  public SchedulingDSLCompilationService(final TypescriptCodeGenerationService typescriptCodeGenerationService)
  throws IOException
  {
    this.typescriptCodeGenerationService = typescriptCodeGenerationService;
    final var schedulingDslCompilerRoot = System.getenv("SCHEDULING_DSL_COMPILER_ROOT");
    final var schedulingDslCompilerCommand = System.getenv("SCHEDULING_DSL_COMPILER_COMMAND");
    final var nodePath = System.getenv("NODE_PATH");
    this.nodeProcess = new ProcessBuilder(nodePath, "--experimental-vm-modules", schedulingDslCompilerCommand)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .directory(new File(schedulingDslCompilerRoot))
        .start();

    final var inputStream = this.nodeProcess.outputWriter();
    inputStream.write("ping\n");
    inputStream.flush();
    if (!Objects.equals(this.nodeProcess.inputReader().readLine(), "pong")) {
      throw new Error("Could not create node subprocess");
    }
  }

  public void close() {
    this.nodeProcess.destroy();
  }

  /**
   * NOTE: This method is not re-entrant (assumes only one call to this method is running at any given time)
   */
  public SchedulingDSLCompilationResult compileSchedulingGoalDSL(final PlanId planId, final String goalTypescript)
  {
    final var missionModelGeneratedCode = JSONObject.quote(this.typescriptCodeGenerationService.generateTypescriptTypesForPlan(planId));

    /*
    * PROTOCOL:
    *   denote this java program as JAVA, and the node subprocess as NODE
    *
    *   JAVA -- stdin --> NODE: { "goalCode": "sourcecode", "missionModelGeneratedCode": "generatedcode" } \n
    *   NODE -- stdout --> JAVA: one of "success\n", "error\n", or "panic\n"
    *   NODE -- stdout --> JAVA: payload associated with success, error, or panic, must be exactly one line terminated with \n
    * */
    final var inputWriter = this.nodeProcess.outputWriter();
    final var outputReader = this.nodeProcess.inputReader();
    final var quotedGoalTypescript = JSONObject.quote(goalTypescript); // adds extra quotes to start and end
    try {
      inputWriter.write("{ \"goalCode\": " + quotedGoalTypescript + ", \"missionModelGeneratedCode\": " + missionModelGeneratedCode + " }\n");
      inputWriter.flush();
      final var status = outputReader.readLine();
      return switch (status) {
        case "panic" -> throw new Error(outputReader.readLine());
        case "error" -> {
          final var output = outputReader.readLine();
          try {
            yield new SchedulingDSLCompilationResult.Error(parseJson(output, SchedulingCompilationError.schedulingErrorJsonP));
          } catch (InvalidJsonException | InvalidEntityException e) {
            throw new Error("Could not parse JSON returned from typescript: ", e);
          }
        }
        case "success" -> {
          final var output = outputReader.readLine();
          try {
            yield new SchedulingDSLCompilationResult.Success(parseJson(output, SchedulingDSL.schedulingJsonP));
          } catch (InvalidJsonException | InvalidEntityException e) {
            throw new Error("Could not parse JSON returned from typescript: ", e);
          }
        }
        default -> throw new Error("scheduling dsl compiler returned unexpected status: " + status);
      };
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  private static <T> T parseJson(final String jsonStr, final JsonParser<T> parser)
  throws InvalidJsonException, InvalidEntityException
  {
    try (final var reader = Json.createReader(new StringReader(jsonStr))) {
      final var requestJson = reader.readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow(reason -> new InvalidEntityException(List.of(reason)));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }

  public sealed interface SchedulingDSLCompilationResult {
    record Success(SchedulingDSL.GoalSpecifier goalSpecifier) implements SchedulingDSLCompilationResult {}
    record Error(List<SchedulingCompilationError.UserCodeError> errors) implements SchedulingDSLCompilationResult {}
  }
}
