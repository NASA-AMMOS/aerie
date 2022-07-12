package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetPlanDatasetStartEnd implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      offset_from_plan_start,
      dataset_duration
    from plan_dataset
    where plan_id = ?
    and dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanDatasetStartEnd(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Pair<Timestamp, Timestamp> get(
      final long planId,
      final long datasetId,
      final Window simulationWindow)
  throws SQLException, NoSuchPlanException {
    this.statement.setLong(1, planId);
    this.statement.setLong(2, datasetId);

    try (final var results = this.statement.executeQuery()) {
      if(results.next()) {
        final Timestamp startOffsetEpoch = simulationWindow.start();
        final var startAsInterval = Interval.parse(results.getString(1)); //startOffset
        Timestamp startOffset =  new Timestamp((Instant) startAsInterval.addTo(startOffsetEpoch.toInstant()));

        final Timestamp datasetEndEpoch = simulationWindow.start();
        final var endAsInterval = Interval.parse(results.getString(2)); //datasetEnd
        Timestamp datasetEnd =  new Timestamp((Instant) endAsInterval.addTo(datasetEndEpoch.toInstant()));
        return Pair.of(startOffset, datasetEnd);
      }
      else {
        throw new SQLException("Dataset id: %d, or plan id: %d, does not exist".formatted(datasetId, planId));
      }
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
