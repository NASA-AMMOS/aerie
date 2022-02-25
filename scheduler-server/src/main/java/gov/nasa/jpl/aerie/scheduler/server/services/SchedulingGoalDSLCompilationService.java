package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.StringReader;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public class SchedulingGoalDSLCompilationService {
  private static final JsonParser<GoalDefinition> schedulingJsonP =
      productP
          .field("abc", stringP)
          .map(Iso.of(
              val -> new GoalDefinition("abc", val),
              GoalDefinition::value));

  public static GoalDefinition compileSchedulingGoalDSL(final String goalTypescript) throws SchedulingGoalDSLCompilationException {
    final var preface = "function goal(): {abc: string}\n";
    final var epilogue = "\nJSON.stringify(goal())";

    final var executionResult = executeTypescript(preface, goalTypescript, epilogue);
    if (!(executionResult instanceof final String executionResultString)) {
      throw new SchedulingGoalDSLCompilationException("Expected result of evaluating typescript to be a string, but got " + executionResult);
    }
    try {
      return parseJson(executionResultString, schedulingJsonP);
    } catch (InvalidJsonException | InvalidEntityException e) {
      throw new SchedulingGoalDSLCompilationException("Could not parse JSON returned from typescript: ", e);
    }
  }

  private static Object executeTypescript(
      final String preface,
      final String goalTypescript,
      final String epilogue
  ) throws SchedulingGoalDSLCompilationException {
    final Object executionResult;
    try {
      executionResult = TypescriptExecutionService.executeTypescript(preface + goalTypescript + epilogue);
    } catch (TypescriptExecutionService.TypescriptExecutionException e) {
      throw new SchedulingGoalDSLCompilationException("Error executing typescript", e);
    } catch (TypescriptExecutionService.TypescriptCompilationException e) {
      throw new SchedulingGoalDSLCompilationException(e.getMessage());
    }
    return executionResult;
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
