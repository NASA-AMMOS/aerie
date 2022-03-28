package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.activityArgumentsP;

/*package-local*/ final class GetActivityAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      a.type,
      ceil(extract(epoch from a.start_offset) * 1000*1000) as start_offset_in_micros,
      a.arguments
    from activity as a
    where a.plan_id = ? and
          a.id = ?
    """;

  private final PreparedStatement statement;

  public GetActivityAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<ActivityInstanceRecord> get(final long planId, final long activityId) throws SQLException {
    this.statement.setLong(1, planId);
    this.statement.setLong(2, activityId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) return Optional.empty();

      final var startOffset = results.getLong("start_offset_in_micros");
      final var type = results.getString("type");
      final var activityArguments = parseActivityArguments(results.getCharacterStream("arguments"));

      return Optional.of(
          new ActivityInstanceRecord(
              activityId,
              type,
              startOffset,
              activityArguments));
    }
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
