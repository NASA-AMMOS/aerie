package gov.nasa.jpl.aerie.scheduler.server.graphql;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.models.ActivityAttributesRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.server.services.GraphQLMerlinService;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;

/**
 * utility methods for parsing graphql scalars as returned by the merlin interface
 */
//TODO: elevate to a merlin-level library to allow parsing in various modules (eg dupl in scheduler...DemuxJson)
public class GraphQLParsers {

  /**
   * the formatting expected in timestamptz scalars returned by graphql queries
   */
  //TODO: what about sub-seconds in plan start_time?
  //TODO: inconsistent with DOY format in Timestamp.fromString, MerlinParsers.timestampP, etc used in merlin-server
  public static final DateTimeFormatter timestampFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  /**
   * the formatting expected in interval scalars returned by graphql queries
   */
  //TODO: inconsistent with bare microseconds used elsewhere
  public static final Pattern intervalPattern = Pattern.compile(
      "^(?<sign>[+-])?" //optional sign prefix, as in +322:21:15
      + "(((?<hr>\\d+):)?" //optional hours field, as in  322:21:15
      + "(?<min>\\d+):)?" //optional minutes field, as in 22:15
      + "(?<sec>\\d+" //required seconds field, as in 15
      + "(\\.\\d*)?)$"); //optional decimal sub-seconds, as in 15. or 15.111

  /**
   * parse the given graphQL formatted timestamptz scalar string (eg 2021-01-01T00:00:00+00:00)
   *
   * @param in the input graphql formatted timestamptz scalar string to parse
   * @return the timestamp object represented by the input string
   */
  //TODO: unify with wherever this translation must already exist in merlin
  public static Timestamp parseGraphQLTimestamp(final String in) {
    return new Timestamp(ZonedDateTime.parse(in, timestampFormat).toInstant());
  }

  /**
   * parse the given graphQL formatted interval scalar string (eg 322:21:15.250)
   *
   * supports up to microsecond precision
   *
   * @param in the input graphql formatted interval scalar string to parse
   * @return the interval object represented by the input string
   */
  //TODO: unify with wherever this translation must already exist in merlin
  public static Interval parseGraphQLInterval(final String in) {

    final var matcher = intervalPattern.matcher(in);
    if (!matcher.matches()) {
      throw new DateTimeParseException("unable to parse HH:MM:SS.sss duration from \"" + in + "\"", in, 0);
    }
    final var signValues = Map.of("+", 1, "-", -1);
    final var sign = Optional.ofNullable(matcher.group("sign")).map(signValues::get).orElse(1);
    final var hr = Optional.ofNullable(matcher.group("hr")).map(Integer::parseInt)
                           .map(Duration::ofHours).orElse(Duration.ZERO);
    final var min = Optional.ofNullable(matcher.group("min")).map(Integer::parseInt)
                            .map(Duration::ofMinutes).orElse(Duration.ZERO);
    final var sec = Optional.ofNullable(matcher.group("sec")).map(Double::parseDouble)
                            .map(s -> (long) (s * 1000 * 1000))//seconds->millis->micros
                            .map(us -> Duration.of(us, ChronoUnit.MICROS))
                            .orElse(Duration.ZERO);
    final var total = hr.plus(min).plus(sec).multipliedBy(sign);
    return Interval.of(total);
  }

  public static final JsonParser<Map<String, SerializedValue>> simulationArgumentsP = mapP(serializedValueP);

  public static final JsonParser<RealDynamics> realDynamicsP
      = productP
      . field("initial", doubleP)
      . field("rate", doubleP)
      . map(
          untuple(RealDynamics::linear),
          $ -> tuple($.initial, $.rate));

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


  public static final JsonParser<Map<String, SerializedValue>> activityArgumentsP = mapP(serializedValueP);

  public static final JsonParser<ActivityAttributesRecord> activityAttributesP = productP
      .optionalField("directiveId", longP)
      .field("arguments", activityArgumentsP)
      .optionalField("computedAttributes", serializedValueP)
      .map(
          untuple(ActivityAttributesRecord::new),
          $ -> tuple($.directiveId(), $.arguments(), $.computedAttributes()));

}
