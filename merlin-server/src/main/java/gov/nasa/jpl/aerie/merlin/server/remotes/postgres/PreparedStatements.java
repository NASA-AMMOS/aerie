package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.SimulationFailure;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import javax.json.Json;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Optional;

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

  public static void setValidationNotices(final PreparedStatement statement, final int parameter, final List<ValidationNotice> notices)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeValidationNotices(notices).toString());
  }

  public static void setFailureReason(final PreparedStatement statement, final int parameter, final SimulationFailure reason)
  throws SQLException
  {
    statement.setString(parameter, reason == null ?
        null :
        MerlinParsers.simulationFailureP.unparse(reason).toString());
  }

  public static Optional<SimulationFailure> getFailureReason(final ResultSet results, final int column)
  throws SQLException
  {
    final var failureJson = results.getString(column);
    return failureJson == null || failureJson.isBlank() ?
        Optional.empty() :
        Optional.of(PreparedStatements.deserializeScheduleFailure(results.getString(column)));
  }

  private static SimulationFailure deserializeScheduleFailure(final String failureJson) {
    try (final var reader = Json.createReader(new ByteArrayInputStream(failureJson.getBytes(StandardCharsets.UTF_8)))) {
      return MerlinParsers.simulationFailureP.parse(reader.readValue()).getSuccessOrThrow();
    }
  }
}
