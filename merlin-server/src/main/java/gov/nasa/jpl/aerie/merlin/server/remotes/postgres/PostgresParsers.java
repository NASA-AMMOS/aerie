package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.SchemaCache;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.*;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;

public final class PostgresParsers {

  public static final JsonParser<Timestamp> pgTimestampP = new JsonParser<>() {
    private static final DateTimeFormatter format =
        new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .toFormatter();

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
          final var instant = LocalDateTime.parse(s.result(), format).atZone(ZoneOffset.UTC);
          return JsonParseResult.success(new Timestamp(instant));
        } catch (DateTimeParseException e) {
          return JsonParseResult.failure("invalid timestamp format "+e);
        }
      } else if (result instanceof JsonParseResult.Failure<?> f) {
        return f.cast();
      } else {
        throw new UnexpectedSubtypeError(JsonParseResult.class, result);
      }
    }

    @Override
    public JsonValue unparse(final Timestamp value) {
      final var s = format.format(value.toInstant().atZone(ZoneOffset.UTC));
      return stringP.unparse(s);
    }
  };

  public static final JsonParser<Pair<String, ValueSchema>> discreteProfileTypeP =
      productP
          .field("type", literalP("discrete"))
          .field("schema", valueSchemaP)
          .map(
              untuple((type, schema) -> Pair.of("discrete", schema)),
              $ -> tuple(Unit.UNIT, $.getRight()));

  public static final JsonParser<Pair<String, ValueSchema>> realProfileTypeP =
      productP
          .field("type", literalP("real"))
          .field("schema", valueSchemaP)
          .map(
              untuple((type, schema) -> Pair.of("real", schema)),
              $ -> tuple(Unit.UNIT, $.getRight()));

  static final JsonParser<Pair<String, ValueSchema>> profileTypeP =
      chooseP(
          discreteProfileTypeP,
          realProfileTypeP);

  public static Duration parseOffset(final ResultSet resultSet, final int index, final Timestamp epoch) throws SQLException {
    final var interval = Interval.parse(resultSet.getString(index));
    final Timestamp end = new Timestamp((Instant)interval.addTo(epoch.toInstant()));
    return Duration.of(epoch.microsUntil(end), Duration.MICROSECONDS);
  }

  public static Duration parseOffset(final ResultSet resultSet, final int index, final Instant epoch) throws SQLException {
    return parseOffset(resultSet, index, new Timestamp(epoch));
  }

  public static final JsonParser<Map<String, SerializedValue>> activityArgumentsP = mapP(serializedValueP);
  public static final JsonParser<Map<String, SerializedValue>> simulationArgumentsP = mapP(serializedValueP);

  public static final JsonParser<ActivityAttributesRecord> activityAttributesP = productP
      .optionalField("directiveId", longP)
      .field("arguments", activityArgumentsP)
      .optionalField("computedAttributes", serializedValueP)
        .map(
            untuple(ActivityAttributesRecord::new),
            $ -> tuple($.directiveId(), $.arguments(), $.computedAttributes()));

  public static final JsonParser<Map<String, Pair<Integer, ValueSchema>>> parameterRecordP =
      mapP(
          productP
              .field("order", intP)
              .field("schema", valueSchemaP));

  public static <V> JsonParseResult<V>
  getJsonColumn(final ResultSet results, final String column, final JsonParser<V> parser) throws SQLException {
    try (final var reader = Json.createReader(results.getCharacterStream(column))) {
      return parser.parse(reader.readValue());
    }
  }
}
