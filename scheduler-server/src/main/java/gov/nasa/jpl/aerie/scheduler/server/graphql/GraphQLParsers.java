package gov.nasa.jpl.aerie.scheduler.server.graphql;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * utility methods for parsing graphql scalars as returned by the merlin interface
 */
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
      "^(((?<hr>\\d+):)?" //optional hours field, as in  322:21:15
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
    final var hr = Optional.ofNullable(matcher.group("hr")).map(Integer::parseInt)
                           .map(Duration::ofHours).orElse(Duration.ZERO);
    final var min = Optional.ofNullable(matcher.group("min")).map(Integer::parseInt)
                            .map(Duration::ofMinutes).orElse(Duration.ZERO);
    final var sec = Optional.ofNullable(matcher.group("sec")).map(Double::parseDouble)
                            .map(s -> (long) (s * 1000 * 1000))//seconds->millis->micros
                            .map(us -> Duration.of(us, ChronoUnit.MICROS))
                            .orElse(Duration.ZERO);
    return Interval.of(hr.plus(min).plus(sec));
  }
}
