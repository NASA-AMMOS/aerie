package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.activityAttributesP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setDuration;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

/*package-local*/ final class PostSpansAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
      insert into span (dataset_id, start_offset, duration, type, attributes)
      values (?, ?::interval, ?::interval, ?, ?::jsonb)
    """;

  private final PreparedStatement statement;

  public PostSpansAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
  }

  public Map<Long, Long> apply(
      final long datasetId,
      final Map<Long, SpanRecord> spans,
      final Timestamp simulationStart
  ) throws SQLException {
    final var ids = spans.keySet().stream().toList();
    for (final var id : ids) {
      final var act = spans.get(id);
      final var startOffset = Duration.of(simulationStart.microsUntil(new Timestamp(act.start())), Duration.MICROSECONDS);

      statement.setLong(1, datasetId);
      setDuration(statement, 2, startOffset);

      if (act.duration().isPresent()) {
        setDuration(statement, 3, act.duration().get());
      } else {
        statement.setNull(3, Types.TIMESTAMP_WITH_TIMEZONE);
      }

      statement.setString(4, act.type());
      statement.setString(5, buildAttributes(act.attributes().directiveId(), act.attributes().arguments(), act.attributes().computedAttributes()));
      statement.addBatch();
    }

    statement.executeBatch();
    final var resultSet = statement.getGeneratedKeys();

    final var simIdToPostgresId = new HashMap<Long, Long>(ids.size());
    for (final var id : ids) {
      if (!resultSet.next()) throw new Error("Not enough generated IDs returned from batch insertion.");
      simIdToPostgresId.put(id, resultSet.getLong(1));
    }

    return simIdToPostgresId;
  }

  private String buildAttributes(final Optional<Long> directiveId, final Map<String, SerializedValue> arguments, final Optional<SerializedValue> returnValue) {
    return activityAttributesP.unparse(new ActivityAttributesRecord(directiveId, arguments, returnValue)).toString();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
