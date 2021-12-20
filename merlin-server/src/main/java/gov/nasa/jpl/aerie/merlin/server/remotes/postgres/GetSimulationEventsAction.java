package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetSimulationEventsAction implements AutoCloseable {
  @Language("SQL") private final String sql = """
        select
          e.real_time,
          e.transaction_index,
          e.causal_time,
          e.topic_index,
          e.task_id,
          e.value
        from event as e
        where
          e.dataset_id = ?
      """;

  private final PreparedStatement statement;

  public GetSimulationEventsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(this.sql);
  }

  public SortedMap<Duration, List<EventGraph<Triple<Integer, SerializedValue, String>>>> get(
      final long datasetId,
      final Timestamp simulationStart) throws SQLException
  {
    this.statement.setLong(1, datasetId);
    final var resultSet = this.statement.executeQuery();

    final var transactionsByTimePoint = readResultSet(resultSet, simulationStart);

    final var eventPoints = new TreeMap<Duration, List<EventGraph<Triple<Integer, SerializedValue, String>>>>();
    transactionsByTimePoint.forEach((time, transactions) -> {
      transactions.forEach(($, value) -> {
        try {
          eventPoints
              .computeIfAbsent(time, x -> new ArrayList<>())
              .add(EventGraphFlattener.unflatten(value));
        } catch (final EventGraphFlattener.InvalidTagException e) {
          throw new Error("Failed to unflatten EventGraph due to invalid tag at time point " + time, e);
        }
      });
    });
    return eventPoints;
  }

  private static Map<Duration, SortedMap<Integer, List<Pair<String, Triple<Integer, SerializedValue, String>>>>>
  readResultSet(final ResultSet resultSet, final Timestamp simulationStart)
  throws SQLException {
    final var nodesByTimePoint = new HashMap<Duration, SortedMap<Integer, List<Pair<String, Triple<Integer, SerializedValue, String>>>>>();
    while (resultSet.next()) {
      final var timePoint = parseOffset(resultSet, 1, simulationStart);
      final var transactionIndex = resultSet.getInt(2);
      final var causalTime = resultSet.getString(3);
      final var topicIndex = resultSet.getInt(4);
      final String activeTask = resultSet.getString(5);
      final var serializedValue = parseSerializedValue(resultSet.getString(6));


      nodesByTimePoint
          .computeIfAbsent(timePoint, x -> new TreeMap<>())
          .computeIfAbsent(transactionIndex, x -> new ArrayList<>())
          .add(Pair.of(
              causalTime,
              Triple.of(topicIndex, serializedValue, activeTask))
          );
    }
    return nodesByTimePoint;
  }

  private static SerializedValue parseSerializedValue(final String value) {
    final SerializedValue serializedValue;
    try (
        final var serializedValueReader = Json.createReader(new StringReader(value))
    ) {
      serializedValue = serializedValueP
          .parse(serializedValueReader.readValue())
          .getSuccessOrThrow();
    }
    return serializedValue;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
