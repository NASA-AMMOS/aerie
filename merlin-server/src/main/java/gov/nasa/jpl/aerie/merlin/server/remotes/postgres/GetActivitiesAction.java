package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.activityArgumentsP;

/*package-local*/ final class GetActivitiesAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      a.id,
      a.type,
      ceil(extract(epoch from a.start_offset) * 1000*1000) as start_offset_in_micros,
      a.arguments
    from activity as a
    where a.plan_id = ?
    """;

  private final PreparedStatement statement;

  public GetActivitiesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<ActivityInstanceRecord> get(final long planId) throws SQLException {
    this.statement.setLong(1, planId);

    final var activities = new ArrayList<ActivityInstanceRecord>();
    try (final var results = this.statement.executeQuery()) {
      while (results.next()) {
        activities.add(
            new ActivityInstanceRecord(
                results.getLong("id"),
                results.getString("type"),
                results.getLong("start_offset_in_micros"),
                parseActivityArguments(results.getCharacterStream("arguments"))));
      }
    }

    return activities;
  }

  private Map<String, SerializedValue> parseActivityArguments(final Reader stream) {
    final var json = Json.createReader(stream).readValue();
    return activityArgumentsP
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error("Corrupt activity arguments cannot be parsed: " + failureReason.reason())
        );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
