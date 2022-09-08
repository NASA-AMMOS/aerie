package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;

public final class PostgresParsers {
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
