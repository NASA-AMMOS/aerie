package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public class SchedulingGoalDSLCompilationService {

  private final Process nodeProcess;

  public SchedulingGoalDSLCompilationService() throws SchedulingGoalDSLCompilationException, IOException {
    final var schedulingDslCompilerRoot = System.getenv("SCHEDULING_DSL_COMPILER_ROOT");
    final var schedulingDslCompilerCommand = System.getenv("SCHEDULING_DSL_COMPILER_COMMAND");
    this.nodeProcess = Runtime.getRuntime().exec(new String[]{"node", schedulingDslCompilerCommand}, null, new File(schedulingDslCompilerRoot));

    try {
      this.nodeProcess.getInputStream();
      this.nodeProcess.getOutputStream();
    } catch (Exception e) {
      throw new SchedulingGoalDSLCompilationException("Could not create node subprocess: ", e);
    }
  }

  public void close() {
    this.nodeProcess.destroy();
  }

  private static final JsonParser<GoalDefinition> schedulingJsonP =
      productP
          .field("abc", stringP)
          .map(Iso.of(
              val -> new GoalDefinition("abc", val),
              GoalDefinition::value));

  public GoalDefinition compileSchedulingGoalDSL(final String goalTypescript, final String goalName)
  throws SchedulingGoalDSLCompilationException, IOException
  {
    /*
    * PROTOCOL:
    *   denote this java program as JAVA, and the node subprocess as NODE
    *
    *   JAVA -- stdin --> NODE: { "source": "sourcecode", "filename": "goalname" } \n
    *   NODE -- stdout --> JAVA: one of "success\n" or "error\n"
    *   NODE -- stdout --> JAVA: payload associated with success or failure, must be exactly one line terminated with \n
    * */
    final var inputWriter = this.nodeProcess.outputWriter();
    final var outputReader = this.nodeProcess.inputReader();

    final var quotedGoalTypescript = JSONObject.quote(goalTypescript); // adds extra quotes to start and end
    inputWriter.write("{ \"source\": " + quotedGoalTypescript + ", \"filename\": \"" + goalName + "\" }\n");
    inputWriter.flush();

    final var status = outputReader.readLine();
    if (status.equals("error")) {
      throw new SchedulingGoalDSLCompilationException(outputReader.readLine());
    }

    if (status.equals("success")) {
      final var output = outputReader.readLine();
      try {
        return parseJson(output, schedulingJsonP);
      } catch (InvalidJsonException | InvalidEntityException e) {
        throw new SchedulingGoalDSLCompilationException("Could not parse JSON returned from typescript: ", e);
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

  record GoalDefinition(String key, String value) {}

  public static class SchedulingGoalDSLCompilationException extends Exception {
    SchedulingGoalDSLCompilationException(final String message, final Exception e) {
      super(message, e);
    }
    SchedulingGoalDSLCompilationException(final String message) {
      super(message);
    }
  }
}
