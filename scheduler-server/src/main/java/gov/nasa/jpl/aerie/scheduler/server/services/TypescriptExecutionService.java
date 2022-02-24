package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import org.jetbrains.annotations.Nullable;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public class TypescriptExecutionService {
  private static final JsonParser<GoalDefinition> schedulingJsonP =
      productP
          .field("abc", stringP)
          .map(Iso.of(
              val -> new GoalDefinition("abc", val),
              GoalDefinition::value));

  public static GoalDefinition executeTypescript(final String goalDefinitionTypescript) throws ScriptException {
    return executeJavascript(compileTypescriptToJavascript(goalDefinitionTypescript));
  }

  private static String compileTypescriptToJavascript(final String typescriptCode) {
    final var temporaryTypescriptFilename = "goal.ts";
    createTemporaryTypescriptFile(typescriptCode, temporaryTypescriptFilename);
    runTypescriptCompiler(temporaryTypescriptFilename);
    return readGeneratedJavascriptFile("goal.js");
  }

  private static String readGeneratedJavascriptFile(final String generatedJavascriptFilename) {
    final String javascriptCode;
    try (final var br = new BufferedReader(new FileReader(generatedJavascriptFilename))) {
      final var sb = new StringBuilder();
      var line = br.readLine();

      while (line != null) {
        sb.append(line);
        sb.append(System.lineSeparator());
        line = br.readLine();
      }
      javascriptCode = sb.toString();
    } catch (IOException e) {
      throw new Error("Error reading generated javascript file", e);
    }
    return javascriptCode;
  }

  private static void runTypescriptCompiler(final String temporaryTypescriptFilename) {
    final var rt = Runtime.getRuntime();
    try {
      final var pr = rt.exec("tsc " + temporaryTypescriptFilename);
      final var i = pr.waitFor();
      if (i != 0) {
        throw new IOException("tsc return status was " + i);
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void createTemporaryTypescriptFile(
      final String typescriptCode,
      final String temporaryTypescriptFilename)
  {
    try (final var out = new PrintWriter(temporaryTypescriptFilename)) {
      out.println(typescriptCode);
    } catch (FileNotFoundException e) {
      throw new Error("Error writing to temporary typescript file", e);
    }
  }

  private static GoalDefinition executeJavascript(final String goalDefinitionJavascript) throws ScriptException {
    final var factory = new NashornScriptEngineFactory();
    final var scriptEngine = factory.getScriptEngine();

    final var json = (String) scriptEngine.eval(goalDefinitionJavascript + "; JSON.stringify(goal())");
    try {
      return parseJson(json, schedulingJsonP);
    } catch (InvalidJsonException | InvalidEntityException e) {
      e.printStackTrace();
      throw new Error("Could not parse JSON returned from javascript: " + e);
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

  record GoalDefinition(String key, String value) {}
}
