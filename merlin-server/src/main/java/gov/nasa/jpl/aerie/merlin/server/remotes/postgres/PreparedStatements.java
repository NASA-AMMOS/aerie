package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.server.http.ValueSchemaJsonParser.valueSchemaP;

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

  public static void setValueSchemaMap(final PreparedStatement statement, final int parameter, final Map<String, ValueSchema> parameters)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeMap(valueSchemaP::unparse, parameters).toString());
  }

  public static void setValueSchemaOrderedMap(final PreparedStatement statement, final int parameter, final Map<String, Pair<Integer, Parameter>> orderedParameters)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeMap(ResponseSerializers::serializeOrderedParameter, orderedParameters).toString());
  }

  public static void setRequiredParameters(final PreparedStatement statement, final int parameter, final List<String> requiredParameters)
  throws SQLException {
    statement.setString(parameter, ResponseSerializers.serializeStringList(requiredParameters).toString());
  }
}
