package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SchedulingDSLCompilationService {

  private final Process nodeProcess;
  private final TypescriptCodeGenerationService typescriptCodeGenerationService;

  public SchedulingDSLCompilationService(final TypescriptCodeGenerationService typescriptCodeGenerationService) throws SchedulingDSLCompilationException, IOException {
    this.typescriptCodeGenerationService = typescriptCodeGenerationService;
    final var schedulingDslCompilerRoot = System.getenv("SCHEDULING_DSL_COMPILER_ROOT");
    final var schedulingDslCompilerCommand = System.getenv("SCHEDULING_DSL_COMPILER_COMMAND");
    this.nodeProcess = Runtime.getRuntime().exec(new String[]{"node", schedulingDslCompilerCommand}, null, new File(schedulingDslCompilerRoot));

    try {
      this.nodeProcess.getInputStream();
      this.nodeProcess.getOutputStream();
    } catch (Exception e) {
      throw new SchedulingDSLCompilationException("Could not create node subprocess: ", e);
    }
  }

  public void close() {
    this.nodeProcess.destroy();
  }

  /**
   * NOTE: This method is not re-entrant (assumes only one call to this method is running at any given time)
   */
  public SchedulingDSL.GoalSpecifier compileSchedulingGoalDSL(final PlanId planId, final String goalTypescript, final String goalName)
  throws SchedulingDSLCompilationException, IOException
  {
    final var generatedCode = JSONObject.quote(this.typescriptCodeGenerationService.generateTypescriptTypesForPlan(planId));

    /*
    * PROTOCOL:
    *   denote this java program as JAVA, and the node subprocess as NODE
    *
    *   JAVA -- stdin --> NODE: { "source": "sourcecode", "filename": "goalname", "generatedCode": "generatedcode" } \n
    *   NODE -- stdout --> JAVA: one of "success\n" or "error\n"
    *   NODE -- stdout --> JAVA: payload associated with success or failure, must be exactly one line terminated with \n
    * */
    final var inputWriter = this.nodeProcess.outputWriter();
    final var outputReader = this.nodeProcess.inputReader();

    final var quotedGoalTypescript = JSONObject.quote(goalTypescript); // adds extra quotes to start and end
    inputWriter.write("{ \"source\": " + quotedGoalTypescript + ", \"filename\": \"" + goalName + "\", \"generatedCode\": " + generatedCode + " }\n");
    inputWriter.flush();

    final var status = outputReader.readLine();
    if (status.equals("error")) {
      throw new SchedulingDSLCompilationException(outputReader.readLine());
    }

    if (status.equals("success")) {
      final var output = outputReader.readLine();
      try {
        return parseJson(output, SchedulingDSL.schedulingJsonP);
      } catch (InvalidJsonException | InvalidEntityException e) {
        throw new SchedulingDSLCompilationException("Could not parse JSON returned from typescript: ", e);
      }
    }

    // Status was neither failure nor success, the protocol has been violated.
    throw new Error("scheduling dsl compiler returned unexpected status: " + status);
  }

  private static <T> T parseJson(final String jsonStr, final JsonParser<T> parser) throws InvalidJsonException, InvalidEntityException {
    try (final var reader = Json.createReader(new StringReader(jsonStr))) {
      final var requestJson = reader.readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow(reason -> new InvalidEntityException(List.of(reason)));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }

  public static class SchedulingDSLCompilationException extends Exception {
    SchedulingDSLCompilationException(final String message, final Exception e) {
      super(message, e);
    }
    SchedulingDSLCompilationException(final String message) {
      super(message);
    }
  }
}
