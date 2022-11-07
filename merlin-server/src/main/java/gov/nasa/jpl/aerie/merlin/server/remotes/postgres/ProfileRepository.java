package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.http.ProfileParsers.realDynamicsP;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;

/*package-local*/ final class ProfileRepository {
  static ProfileSet getProfiles(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, Optional<RealDynamics>>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, Optional<SerializedValue>>>>>();

    final var profileRecords = getProfileRecords(connection, datasetId);
    for (final var record : profileRecords) {
      switch (record.type().getLeft()) {
        case "real" -> realProfiles.put(
          record.name(),
          Pair.of(
              record.type().getRight(),
              getRealProfileSegments(connection, record.datasetId(), record.id(), record.duration())
          )
        );
        case "discrete" -> discreteProfiles.put(
            record.name(),
            Pair.of(
                record.type().getRight(),
                getDiscreteProfileSegments(connection, record.datasetId(), record.id(), record.duration())
            )
        );
        default -> throw new Error("Unrecognized profile type");
      }
    }

    return new ProfileSet(realProfiles, discreteProfiles);
  }

  static Map<String, ValueSchema> getProfileSchemas(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    final var results = new HashMap<String, ValueSchema>();

    final var profileRecords = getProfileRecords(connection, datasetId);
    for (final var record : profileRecords) {
      results.put(record.name(), record.type().getRight());
    }

    return results;
  }

  static List<PlanDatasetRecord> getAllPlanDatasetsForPlan(final Connection connection, final PlanId planId, final Timestamp planStartTime) throws SQLException {
    try (final var getPlanDatasetsAction = new GetPlanDatasetsAction(connection)) {
      return getPlanDatasetsAction.get(planId, planStartTime);
    }
  }

  static List<ProfileRecord> getProfileRecords(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    try (final var getProfilesAction = new GetProfilesAction(connection)) {
      return getProfilesAction.get(datasetId);
    }
  }

  static List<Pair<Duration, Optional<RealDynamics>>> getRealProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Duration profileDuration
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, profileDuration, realDynamicsP);
    }
  }

  static List<Pair<Duration, Optional<SerializedValue>>> getDiscreteProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Duration profileDuration
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, profileDuration, serializedValueP);
    }
  }

  static void postResourceProfiles(
      final Connection connection,
      final long datasetId,
      final ProfileSet profileSet
  ) throws SQLException
  {
    try (final var postProfilesAction = new PostProfilesAction(connection)) {
      final var profileRecords = postProfilesAction.apply(
          datasetId,
          profileSet.realProfiles(),
          profileSet.discreteProfiles());
      postProfileSegments(
          connection,
          datasetId,
          profileRecords,
          profileSet);
    }
  }

  private static void postProfileSegments(
      final Connection connection,
      final long datasetId,
      final Map<String, ProfileRecord> records,
      final ProfileSet profileSet
  ) throws SQLException {
    final var realProfiles = profileSet.realProfiles();
    final var discreteProfiles = profileSet.discreteProfiles();
    for (final var entry : records.entrySet()) {
      final ProfileRecord record =  entry.getValue();
      final var resource =  entry.getKey();
      switch (record.type().getLeft()) {
        case "real" -> postRealProfileSegments(
            connection,
            datasetId,
            record,
            realProfiles.get(resource).getRight());
        case "discrete" -> postDiscreteProfileSegments(
            connection,
            datasetId,
            record,
            discreteProfiles.get(resource).getRight());
        default -> throw new Error("Unrecognized profile type " + record.type().getLeft());
      }
    }
  }

  private static void postRealProfileSegments(
      final Connection connection,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, Optional<RealDynamics>>> segments
  ) throws SQLException {
    try (final var postProfileSegmentsAction = new PostProfileSegmentsAction(connection)) {
      postProfileSegmentsAction.apply(datasetId, profileRecord, segments, realDynamicsP);
    }
  }

  private static void postDiscreteProfileSegments(
      final Connection connection,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, Optional<SerializedValue>>> segments
  ) throws SQLException {
    try (final var postProfileSegmentsAction = new PostProfileSegmentsAction(connection)) {
      postProfileSegmentsAction.apply(datasetId, profileRecord, segments, serializedValueP);
    }
  }
}
