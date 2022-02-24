package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import javax.script.ScriptException;
import java.io.StringReader;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public class JavascriptExecutionService {
  private static final JsonParser<GoalDefinition> schedulingJsonP =
      productP
        .field("abc", stringP)
        .map(Iso.of(val -> new GoalDefinition("abc", val),
                    GoalDefinition::value));

  record GoalDefinition(String key, String value) {}

  static GoalDefinition executeJavascript(String fooFunction) throws ScriptException {
    final var factory = new NashornScriptEngineFactory();
    final var scriptEngine = factory.getScriptEngine();

    final var json = (String) scriptEngine.eval(fooFunction + "; JSON.stringify(foo())");
    try {
      return parseJson(json, schedulingJsonP);
    } catch (InvalidJsonException | InvalidEntityException e) {
      e.printStackTrace();
      throw new Error("Could not parse JSON returned from javascript: " + e);
    }
  }

  private static <T> T parseJson(final String jsonStr, final JsonParser<T> parser)
  throws InvalidJsonException, InvalidEntityException {
    try(final var reader = Json.createReader(new StringReader(jsonStr))) {
      final var requestJson = reader.readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow(reason -> new InvalidEntityException(List.of(reason)));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }
}
