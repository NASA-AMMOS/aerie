package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

public final class PreparedStatements {
  private PreparedStatements() {}

  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      new DateTimeFormatterBuilder()
          .appendPattern("uuuu-MM-dd HH:mm:ss")
          .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
          .appendOffset("+HH:mm:ss", "+00")
          .toFormatter();

  public static void setTimestamp(final PreparedStatement statement, final int parameter, final Timestamp argument)
  throws SQLException {
    statement.setString(parameter, TIMESTAMP_FORMAT.format(argument.time()));
  }

  public static void setParameters(final PreparedStatement statement, final int parameter, final List<Parameter> parameters)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeParameters(parameters).toString());
  }

  public static void setRequiredParameters(final PreparedStatement statement, final int parameter, final List<String> requiredParameters)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeStringList(requiredParameters).toString());
  }

  public static void setValidationNotices(final PreparedStatement statement, final List<Integer> parameters, final List<ValidationNotice> notices)
  throws SQLException {
    final var serializedNotices = ResponseSerializers.serializeValidationNotices(notices).toString();
    for (final var p : parameters) statement.setString(p, serializedNotices);
  }
}
