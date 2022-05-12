package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.server.http.ValueSchemaJsonParser.valueSchemaP;

public final class PostgresParsers {
  public static final JsonParser<Pair<String, ValueSchema>> discreteProfileTypeP =
      productP
          .field("type", literalP("discrete"))
          .field("schema", valueSchemaP)
          .map(Iso.of(
              untuple((type, schema) -> Pair.of("discrete", schema)),
              $ -> tuple(Unit.UNIT, $.getRight())));

  public static final JsonParser<Pair<String, ValueSchema>> realProfileTypeP =
      productP
          .field("type", literalP("real"))
          .map(Iso.of(
              type -> Pair.of("real", null),
              $ -> Unit.UNIT));

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
        .map(Iso.of(
            untuple(ActivityAttributesRecord::new),
            $ -> tuple($.directiveId(), $.arguments(), $.computedAttributes())
        ));
}
