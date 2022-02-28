package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public class SchedulingGoalDSLCompilationService {

  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final InputStream errorStream;
  private final Process nodeProcess;

  public SchedulingGoalDSLCompilationService() throws SchedulingGoalDSLCompilationException, IOException {
    final var schedulingDslCompilerPath = System.getenv("SCHEDULING_DSL_COMPILER_PATH");
    final var commands = new String[] {"node", schedulingDslCompilerPath};
    this.nodeProcess = Runtime.getRuntime().exec(commands);
    try {
      this.inputStream = this.nodeProcess.getInputStream();
      this.outputStream = this.nodeProcess.getOutputStream();
      this.errorStream = this.nodeProcess.getErrorStream();
    } catch (Exception e) {
      throw new SchedulingGoalDSLCompilationException("Could not create node subprocess: ", e);
    }
    System.out.println("SchedulingGoalDSLCompilerService created");
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
    var inputWriter = new OutputStreamWriter(this.outputStream, StandardCharsets.UTF_8);
    var outputReader = new BufferedReader(new InputStreamReader(this.inputStream));
    var errorReader = new BufferedReader(new InputStreamReader(this.errorStream));

    inputWriter.write("{ \"source\": \"" + goalTypescript + "\", \"filename\": \"" + goalName + "\" }\n");

    var error = errorReader.readLine();
    var output = outputReader.readLine();
    System.out.println(output);

    if (error != null) {
      throw new SchedulingGoalDSLCompilationException(error);
    }

    try {
      return parseJson(output, schedulingJsonP);
    } catch (InvalidJsonException | InvalidEntityException e) {
      throw new SchedulingGoalDSLCompilationException("Could not parse JSON returned from typescript: ", e);
    }
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
