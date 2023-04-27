package gov.nasa.jpl.aerie.scheduler.server.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GraphQLParsersTest {

  public static Stream<Arguments> parseGraphQLTimestamp() {
    return Stream.of(
        Arguments.of("2021-01-01T00:00:00+00:00", Timestamp.fromString("2021-001T00:00:00")),
        Arguments.of("2021-12-31T23:59:59+00:00", Timestamp.fromString("2021-365T23:59:59")));
  }

  public static Stream<Arguments> parseGraphQLInterval() {
    return Stream.of(
        Arguments.of("-322:21:15.111", Interval.parse("PT-322H-21M-15.111S")),
        Arguments.of("+322:21:15.111", Interval.parse("PT322H21M15.111S")),
        Arguments.of("322:21:15.111", Interval.parse("PT322H21M15.111S")),
        Arguments.of("322:21:15.", Interval.parse("PT322H21M15S")),
        Arguments.of("322:21:15", Interval.parse("PT322H21M15S")),
        Arguments.of("+322:21:15", Interval.parse("PT322H21M15S")),
        Arguments.of("-322:21:15", Interval.parse("PT-322H-21M-15S")),
        Arguments.of("21:15.111", Interval.parse("PT21M15.111S")),
        Arguments.of("+21:15.111", Interval.parse("PT21M15.111S")),
        Arguments.of("-21:15.111", Interval.parse("PT-21M-15.111S")),
        Arguments.of("15.111", Interval.parse("PT15.111S")),
        Arguments.of("15.", Interval.parse("PT15S")),
        Arguments.of("15", Interval.parse("PT15S")),
        Arguments.of("-15", Interval.parse("PT-15S")),
        Arguments.of("+15", Interval.parse("PT15S")));
  }

  @ParameterizedTest
  @MethodSource
  void parseGraphQLTimestamp(String input, Timestamp expected) {
    final var actual = GraphQLParsers.parseGraphQLTimestamp(input);
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource
  void parseGraphQLInterval(String input, Interval expected) {
    final var actual = GraphQLParsers.parseGraphQLInterval(input);
    assertEquals(expected, actual);
  }
}
