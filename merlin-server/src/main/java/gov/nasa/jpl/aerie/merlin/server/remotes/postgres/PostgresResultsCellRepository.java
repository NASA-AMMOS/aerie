package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import org.apache.commons.lang3.tuple.Pair;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.realDynamicsP;

public final class PostgresResultsCellRepository implements ResultsCellRepository {
  private final DataSource dataSource;

  // TODO: Add actual model revisions and take as parameter
  private static final long MODEL_REVISION = -1;

  public PostgresResultsCellRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public ResultsProtocol.OwnerRole allocate(final String planIdString, final long planRevision) {
    // TODO: We should really address the fact that the plan ID is a string in merlin, but stored as a long
    final var planId = Long.parseLong(planIdString);
    try (final var connection = this.dataSource.getConnection()) {
      final var planStart = getPlan(connection, planId).startTimestamp;
      // TODO: At the time of writing, simulation starts at the plan start every time
      //       When that changes, we will need to update the simulation start here as well
      final var simulationStart = planStart;
      var simulation$ = getSimulation(connection, planId);

      final SimulationRecord simulation;
      if (simulation$.isPresent()) {
        simulation = simulation$.get();
      } else {
        simulation = createSimulation(connection, planId);
      }

      // Cancel datasets with lower revisions
      cancelStaleDatasets(connection, simulation);

      final var dataset = instantiateDataset(
          connection,
          simulation,
          MODEL_REVISION,
          planRevision,
          planStart,
          simulationStart);

      return new PostgresResultsCell(this.dataSource, dataset.id());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to allocation simulation cell", ex);
    } catch (final NoSuchPlanException ex) {
      throw new Error("Cannot allocate simulation cell for nonexistent plan", ex);
    }
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final String planIdString, final long planRevision) {
    // TODO: We should really address the fact that the plan ID is a string in merlin, but stored as a long
    final var planId = Long.parseLong(planIdString);
    try (final var connection = this.dataSource.getConnection()) {
      final var simulation$ = getSimulation(connection, planId);
      if (simulation$.isEmpty()) return Optional.empty();
      final var simulation = simulation$.get();

      final var dataset$ = getDataset(connection, simulation.id(), MODEL_REVISION, planRevision, simulation.revision());
      if (dataset$.isEmpty()) return Optional.empty();
      final var dataset = dataset$.get();

      return Optional.of(new PostgresResultsCell(this.dataSource, dataset.id()));
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get simulation", ex);
    }
  }

  @Override
  public void deallocate(final String planIdString, final long planRevision) {
    // TODO: We should really address the fact that the plan ID is a string in merlin, but stored as a long
    final var planId = Long.parseLong(planIdString);
    try (final var connection = this.dataSource.getConnection()) {
      final var simulation$ = getSimulation(connection, planId);
      if (simulation$.isEmpty()) return;
      final var simulation = simulation$.get();

      deleteDataset(connection, simulation, MODEL_REVISION, planRevision);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete simulation", ex);
    }
  }

  /* Database accessors */
  private static Optional<SimulationRecord> getSimulation(
      final Connection connection,
      final long planId
  ) throws SQLException
  {
    try (final var getSimulationAction = new GetSimulationAction(connection)) {
      return getSimulationAction.get(planId);
    }
  }

  private static Optional<DatasetRecord> getDataset(
      final Connection connection,
      final long datasetId
  ) throws SQLException
  {
    try (final var getDatasetAction = new GetDatasetAction(connection)) {
      return getDatasetAction.get(datasetId);
    }
  }

  private static Optional<DatasetRecord> getDataset(
      final Connection connection,
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision
  ) throws SQLException
  {
    try (final var getDatasetMetadataAction = new GetDatasetMetadataAction(connection)) {
      final var datasetId$ = getDatasetMetadataAction
          .get(simulationId, modelRevision, planRevision, simulationRevision)
          .map(DatasetMetadataRecord::datasetID);
      if (datasetId$.isEmpty()) return Optional.empty();
      return getDataset(connection, datasetId$.get());
    }
  }

  private static SimulationRecord createSimulation(
      final Connection connection,
      final long planId
  ) throws SQLException
  {
    try (final var createSimulationAction = new CreateSimulationAction(connection)) {
      return createSimulationAction.apply(planId);
    }
  }

  private static DatasetRecord instantiateDataset(
      final Connection connection,
      final SimulationRecord simulation,
      final long modelRevision,
      final long planRevision,
      final Timestamp planStart,
      final Timestamp simulationStart
  ) throws SQLException
  {
    final var partitionId = UUID.randomUUID().toString().replaceAll("-", "");
    final var profileSegmentPartitionTable = String.format("profile_segment_%s", partitionId);
    final var spanPartitionTable = String.format("span_%s", partitionId);

    final var dataset = createDataset(
        connection,
        simulation.planId(),
        planStart,
        simulationStart,
        profileSegmentPartitionTable,
        spanPartitionTable);
    createPartitionTables(connection, dataset.id(), profileSegmentPartitionTable, spanPartitionTable);
    createSimulationDataset(
        connection,
        simulation.id(),
        dataset.id(),
        modelRevision,
        planRevision,
        simulation.revision());

      return dataset;
  }

  private static DatasetRecord createDataset(
      final Connection connection,
      final long planId,
      final Timestamp planStart,
      final Timestamp simulationStart,
      final String profileSegmentPartitionTable,
      final String spanPartitionTable
  ) throws SQLException {
    try (final var createDatasetAction = new CreateDatasetAction(connection)) {
      return createDatasetAction.apply(
          planId,
          planStart,
          simulationStart,
          profileSegmentPartitionTable,
          spanPartitionTable);
    }
  }

  private static void createPartitionTables(
      final Connection connection,
      final long datasetId,
      final String profileSegmentPartitionName,
      final String spanPartitionName
  ) throws SQLException {
    try (
          final var createProfileSegmentPartitionTableAction =
              new CreateProfileSegmentPartitionTableAction(connection, datasetId, profileSegmentPartitionName);
          final var createSpanPartitionTableAction =
              new CreateSpanPartitionTableAction(connection, datasetId, spanPartitionName)
        ) {
      createProfileSegmentPartitionTableAction.apply();
      createSpanPartitionTableAction.apply();
    }
  }

  private static void createSimulationDataset(
      final Connection connection,
      final long simulationId,
      final long datasetId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision
  ) throws SQLException {
    try (final var createSimulationDatasetAction = new CreateSimulationDatasetAction(connection)) {
      createSimulationDatasetAction.apply(
          simulationId,
          datasetId,
          modelRevision,
          planRevision,
          simulationRevision);
    }
  }

  private static void cancelStaleDatasets(
      final Connection connection,
      final SimulationRecord simulation
  ) throws SQLException
  {
    try (final var getDatasetsForSimulationAction = new GetDatasetsForSimulationAction(connection)) {
      final var simulationDatasets = getDatasetsForSimulationAction.get(simulation);
      for (final var simulationDataset : simulationDatasets) {
        if (simulationDataset.simulationRevision() < simulation.revision()) {
          try {
            cancelSimulation(connection, simulationDataset.datasetID());
          } catch (final NoSuchDatasetException ex) {
            // Another process must have deleted it, nothing to do
          }
        }
      }
    }
  }

  private static void cancelSimulation(
      final Connection connection,
      final long datasetId
  ) throws SQLException, NoSuchDatasetException
  {
    try (final var cancelSimulationAction = new CancelSimulationAction(connection)) {
      cancelSimulationAction.apply(datasetId);
    }
  }

  private static boolean deleteDataset(
      final Connection connection,
      final SimulationRecord simulation,
      final long modelRevision,
      final long planRevision)
  throws SQLException
  {
    final var dataset$ = getDataset(connection, simulation.id(), modelRevision, planRevision, simulation.revision());
    if (dataset$.isEmpty()) return false;
    final var dataset = dataset$.get();

    try (
        final var deleteSimulationDatasetAction = new DeleteSimulationDatasetAction(connection);
        final var deleteDatasetAction = new DeleteDatasetAction(connection)
    ) {
      return deleteSimulationDatasetAction.apply(simulation.id(), modelRevision, planRevision, simulation.revision()) &&
             deleteDatasetAction.apply(dataset.id());
    }
  }

  private static void failSimulation(
      final Connection connection,
      final long datasetId,
      final String reason
  ) throws SQLException, NoSuchDatasetException
  {
    try (final var setSimulationStateAction = new SetSimulationStateAction(connection)) {
      setSimulationStateAction.apply(datasetId, new State.Failed(reason));
    }
  }

  private static Optional<State> getSimulationState(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    final var dataset$ = getDataset(connection, datasetId);
    if (dataset$.isEmpty()) return Optional.empty();
    final var dataset = dataset$.get();
    return Optional.of(
        switch (dataset.state()) {
          case "incomplete" -> new ResultsProtocol.State.Incomplete();
          case "failed" -> new ResultsProtocol.State.Failed(dataset.reason());
          case "success" -> new ResultsProtocol.State.Success(getSimulationResults(connection, dataset));
          default -> throw new Error(String.format("Unexpected simulation state %s", dataset.state()));
        });
  }

  private static SimulationResults getSimulationResults(
      final Connection connection,
      final DatasetRecord dataset
  ) throws SQLException {
    final var simulationStart = getSimulationStart(connection, dataset);
    final var startTimestamp = new Timestamp(simulationStart);

    final var profiles = getProfiles(connection, dataset.id(), startTimestamp);
    final var realProfiles = profiles.getLeft();
    final var discreteProfiles = profiles.getRight();

    final var activities = getSimulatedActivities(connection, dataset.id(), startTimestamp);

    // TODO: Currently we don't store unfinished activities, but when we do we'll have to update this
    final Map<String, SerializedActivity> unfinishedActivities = Map.of();

    return new SimulationResults(
        realProfiles,
        discreteProfiles,
        activities,
        unfinishedActivities,
        simulationStart
    );
  }

  private static Map<String, SimulatedActivity> getSimulatedActivities(
      final Connection connection,
      final long datasetId,
      final Timestamp startTime
  ) throws SQLException {
    try (final var getSimulatedActivitiesAction = new GetSimulatedActivitiesAction(connection)) {
      final var activities = getSimulatedActivitiesAction.get(datasetId, startTime);
      final var pgIdToSimId = liftDirectiveIds(activities);

      // Remap all activity IDs to reflect lifted directive IDs
      final var processedActivities = new HashMap<String, SimulatedActivity>(activities.size());
      for (final var entry : activities.entrySet()) {
        final var id = entry.getKey();
        final var activity = entry.getValue();

        processedActivities.put(pgIdToSimId.get(id), new SimulatedActivity(
            activity.type,
            activity.parameters,
            activity.start,
            activity.duration,
            pgIdToSimId.get(activity.parentId),
            activity.childIds.stream().map(pgIdToSimId::get).collect(Collectors.toList()),
            activity.directiveId
        ));
      }

      return processedActivities;
    }
  }

  private static HashMap<String, String> liftDirectiveIds(final Map<String, SimulatedActivity> activities) {
    final var pgIdToSimId = new HashMap<String, String>(activities.size());
    final var simIds = new HashSet<String>(activities.size());

    for (final var id : activities.keySet()) {
      final var activity = activities.get(id);
      if (activity.directiveId.isEmpty()) continue;

      final var directiveId = activity.directiveId.get();
      pgIdToSimId.put(id, directiveId);
      simIds.add(directiveId);
    }

    var counter = 1;
    for (final var id : activities.keySet()) {
      final var activity = activities.get(id);
      if (activity.directiveId.isPresent()) continue;

      String newId;
      do {
        newId = Integer.toString(counter++);
      } while (simIds.contains(newId));

      pgIdToSimId.put(id, newId);
      simIds.add(newId);
    }
    return pgIdToSimId;
  }

  private static Instant getSimulationStart(
      final Connection connection,
      final DatasetRecord dataset
  ) throws SQLException {
    try {
      return getPlan(connection, dataset.planId())
          .startTimestamp
          .plusMicros(dataset.offsetFromPlanStart().dividedBy(Duration.MICROSECONDS))
          .time
          .toInstant();
    } catch (final NoSuchPlanException ex) {
      throw new Error("Plan for simulation dataset with ID " + dataset.id() + " no longer exists.");
    }
  }

  private static Plan getPlan(
      final Connection connection,
      final long planId
  ) throws SQLException, NoSuchPlanException {
    try (final var getPlanAction = new GetPlanAction(connection)) {
      return getPlanAction.get(planId);
    }
  }

  private static Pair<
      Map<String, List<Pair<Duration, RealDynamics>>>,
      Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>>
  getProfiles(
      final Connection connection,
      final long datasetId,
      final Timestamp simulationStart)
  throws SQLException {
    final var realProfiles = new HashMap<String, List<Pair<Duration, RealDynamics>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();

    final var profileRecords = getProfileRecords(connection, datasetId);
    for (final var record : profileRecords) {
      switch (record.type().getLeft()) {
        case "real" -> realProfiles.put(record.name(), getRealProfileSegments(connection, record.datasetId(), record.id(), simulationStart));
        case "discrete" -> discreteProfiles.put(record.name(),
                                                Pair.of(
                                                    record.type().getRight(),
                                                    getDiscreteProfileSegments(connection, record.datasetId(), record.id(), simulationStart)));
        default -> throw new Error("Unrecognized profile type");
      }
    }

    return Pair.of(realProfiles, discreteProfiles);
  }

  private static List<ProfileRecord> getProfileRecords(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    try (final var getProfilesAction = new GetProfilesAction(connection)) {
      return getProfilesAction.get(datasetId);
    }
  }

  private static List<Pair<Duration, RealDynamics>> getRealProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, simulationStart, realDynamicsP);
    }
  }

  private static List<Pair<Duration, SerializedValue>> getDiscreteProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, simulationStart, serializedValueP);
    }
  }

  private static void postSimulationResults(
      final Connection connection,
      final long datasetId,
      final SimulationResults results
  ) throws SQLException, NoSuchDatasetException {
    final var simulationStart = new Timestamp(results.startTime);
    postResourceProfiles(connection, datasetId, results.realProfiles, results.discreteProfiles, simulationStart);
    postSimulatedActivities(connection, datasetId, results.simulatedActivities, simulationStart);

    try (final var setSimulationStateAction = new SetSimulationStateAction(connection)) {
      setSimulationStateAction.apply(datasetId, new State.Success(results));
    }
  }

  private static void postSimulatedActivities(
      final Connection connection,
      final long datasetId,
      final Map<String, SimulatedActivity> simulatedActivities,
      final Timestamp simulationStart
  ) throws SQLException {
    try (
        final var postSimulatedActivitiesAction = new PostSimulatedActivitiesAction(connection);
        final var updateSimulatedActivityParentsAction = new UpdateSimulatedActivityParentsAction(connection)
    ) {
      final var simIdToPgId = postSimulatedActivitiesAction.apply(
          datasetId,
          simulatedActivities,
          simulationStart);
      updateSimulatedActivityParentsAction.apply(
          datasetId,
          simulatedActivities,
          simIdToPgId);
    }
  }

  private static void postResourceProfiles(
      final Connection connection,
      final long datasetId,
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var postProfilesAction = new PostProfilesAction(connection)) {
      final var profileRecords = postProfilesAction.apply(
          datasetId,
          realProfiles,
          discreteProfiles);
      postProfileSegments(
          connection,
          datasetId,
          profileRecords,
          realProfiles,
          discreteProfiles,
          simulationStart);
    }
  }

  private static void postProfileSegments(
      final Connection connection,
      final long datasetId,
      final Map<String, ProfileRecord> records,
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Timestamp simulationStart
  ) throws SQLException {
    for (final var resource : records.keySet()) {
      final ProfileRecord record = records.get(resource);
      switch (record.type().getLeft()) {
        case "real" -> postRealProfileSegments(
            connection,
            datasetId,
            record,
            realProfiles.get(resource),
            simulationStart);
        case "discrete" -> postDiscreteProfileSegments(
            connection,
            datasetId,
            record,
            discreteProfiles.get(resource).getRight(),
            simulationStart);
        default -> throw new Error("Unrecognized profile type " + record.type().getLeft());
      }
    }
  }

  private static void postRealProfileSegments(
      final Connection connection,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, RealDynamics>> segments,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var postProfileSegmentsAction = new PostProfileSegmentsAction(connection)) {
      postProfileSegmentsAction.apply(datasetId, profileRecord, segments, simulationStart, realDynamicsP);
    }
  }

  private static void postDiscreteProfileSegments(
      final Connection connection,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, SerializedValue>> segments,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var postProfileSegmentsAction = new PostProfileSegmentsAction(connection)) {
      postProfileSegmentsAction.apply(datasetId, profileRecord, segments, simulationStart, serializedValueP);
    }
  }

  public static final class PostgresResultsCell implements ResultsProtocol.OwnerRole {
    private final DataSource dataSource;
    private final long datasetId;

    public PostgresResultsCell(
        final DataSource dataSource,
        final long datasetId
    ) {
      this.dataSource = dataSource;
      this.datasetId = datasetId;
    }

    @Override
    public State get() {
      try (final var connection = dataSource.getConnection()) {
        return getSimulationState(connection, datasetId)
            .orElseThrow(() -> new Error("Dataset corrupted"));
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to get dataset", ex);
      }
    }

    @Override
    public void cancel() {
      try (final var connection = dataSource.getConnection()) {
        cancelSimulation(connection, datasetId);
      } catch(final SQLException ex) {
        throw new DatabaseException("Failed to cancel simulation", ex);
      } catch (final NoSuchDatasetException ex) {
        // A cell should only be created for a valid, existing dataset
        // A dataset should only be deleted by its cell
        throw new Error("Cell references nonexistent simulation dataset");
      }
    }

    @Override
    public boolean isCanceled() {
      try (final var connection = dataSource.getConnection()) {
        return getDataset(
            connection,
            datasetId)
            .map(DatasetRecord::canceled)
            .orElseThrow(() -> new Error("Dataset corrupted"));
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to check cancellation status", ex);
      }
    }

    @Override
    public void succeedWith(final SimulationResults results) {
      try (final var connection = dataSource.getConnection()) {
        postSimulationResults(connection, datasetId, results);
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to store simulation results", ex);
      } catch (final NoSuchDatasetException ex) {
        // A cell should only be created for a valid, existing dataset
        // A dataset should only be deleted by its cell
        throw new Error("Cell references nonexistent simulation dataset");
      }
    }

    @Override
    public void failWith(final String reason) {
      try (final var connection = dataSource.getConnection()) {
        failSimulation(connection, datasetId, reason);
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to update simulation state to failure", ex);
      } catch (final NoSuchDatasetException ex) {
        // A cell should only be created for a valid, existing dataset
        // A dataset should only be deleted by its cell
        throw new Error("Cell references nonexistent simulation dataset");
      }
    }
  }
}
