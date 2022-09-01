package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.SchemaCache;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.format.DateTimeParseException;

import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public abstract class MerlinParsers {
  private MerlinParsers() {}

  public static final JsonParser<Timestamp> timestampP = new JsonParser<>() {
    @Override
    public JsonObject getSchema(final SchemaCache anchors) {
      return Json
          .createObjectBuilder(stringP.getSchema())
          .add("format", "date-time")
          .build();
    }

    @Override
    public JsonParseResult<Timestamp> parse(final JsonValue json) {
      final var result = stringP.parse(json);
      if (result instanceof JsonParseResult.Success<String> s) {
        try {
          return JsonParseResult.success(Timestamp.fromString(s.result()));
        } catch (DateTimeParseException e) {
          return JsonParseResult.failure("invalid timestamp format");
        }
      } else if (result instanceof JsonParseResult.Failure<?> f) {
        return f.cast();
      } else {
        throw new UnexpectedSubtypeError(JsonParseResult.class, result);
      }
    }

    @Override
    public JsonValue unparse(final Timestamp value) {
      return stringP.unparse(value.toString());
    }
  };

  public static final JsonParser<Duration> durationP
      = longP
      . map(
          microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
          duration -> duration.in(Duration.MICROSECONDS));

  public static final JsonParser<ActivityInstanceId> activityInstanceIdP
      = longP
      . map(
          ActivityInstanceId::new,
          ActivityInstanceId::id);

  public static final JsonParser<PlanId> planIdP
      = longP
      . map(
          PlanId::new,
          PlanId::id);
}
