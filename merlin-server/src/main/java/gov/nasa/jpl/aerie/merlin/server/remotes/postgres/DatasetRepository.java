package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/*package-local*/ final class DatasetRepository {
  static DatasetRecord createDataset(
      final Connection connection,
      final long planId,
      final Timestamp planStart,
      final Timestamp datasetStart
  ) throws SQLException {
    try (final var createDatasetAction = new CreateDatasetAction(connection)) {
      return createDatasetAction.apply(
          planId,
          planStart,
          datasetStart);
    }
  }

  static Optional<DatasetRecord> getDataset(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    try (final var getDatasetAction = new GetDatasetAction(connection)) {
      return getDatasetAction.get(datasetId);
    }
  }

  static boolean deleteDataset(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    try (final var deleteDatasetAction = new DeleteDatasetAction(connection)) {
      return deleteDatasetAction.apply(datasetId);
    }
  }
}
