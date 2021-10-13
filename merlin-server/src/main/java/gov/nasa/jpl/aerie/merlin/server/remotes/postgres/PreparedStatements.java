package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;

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
    statement.setString(parameter, TIMESTAMP_FORMAT.format(argument.time));
  }

  public static void setSerializedValue(final PreparedStatement statement, final int parameter, final SerializedValue argument)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeParameter(argument).toString());
  }

  public static void setValueSchemaMap(final PreparedStatement statement, final int parameter, final Map<String, ValueSchema> parameters)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeMap(ResponseSerializers::serializeValueSchema, parameters).toString());
  }
}
