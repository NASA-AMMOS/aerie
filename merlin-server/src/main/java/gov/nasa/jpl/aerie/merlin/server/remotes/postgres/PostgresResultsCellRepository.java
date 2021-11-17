package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
      final var planStart = getPlan(connection, planId).startTime();
      // TODO: At the time of writing, simulation starts at the plan start every time
      //       When that changes, we will need to update the simulation start here as well
      final var simulationStart = planStart;
      var simulation$ = getSimulation(connection, planId);

      final SimulationRecord simulation;
      if (simulation$.isPresent()) {
        simulation = simulation$.get();
      } else {
        simulation = createSimulation(connection, planId, Map.of());
      }

      cancelStaleDatasets(connection, simulation.id());

      final var dataset = instantiateDataset(
          connection,
          simulation,
          MODEL_REVISION,
          planRevision,
          planStart,
          simulationStart);

      return new PostgresResultsCell(
          this.dataSource,
          simulation.id(),
          dataset.id(),
          MODEL_REVISION,
          planRevision,
          simulation.revision(),
          planStart);
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
      final var planStart = getPlan(connection, planId).startTime();

      final var simulation$ = getSimulation(connection, planId);
      if (simulation$.isEmpty()) return Optional.empty();
      final var simulation = simulation$.get();

      final var datasetId$ = getSimulationDatasetRecord(
          connection,
          simulation.id(),
          MODEL_REVISION,
          planRevision,
          simulation.revision()
      ).map(SimulationDatasetRecord::datasetID);

      if (datasetId$.isEmpty()) return Optional.empty();
      final var datasetId = datasetId$.get();

      return Optional.of(new PostgresResultsCell(this.dataSource,
                                                 simulation.id(),
                                                 datasetId,
                                                 MODEL_REVISION,
                                                 planRevision,
                                                 simulation.revision(),
                                                 planStart));
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get simulation", ex);
    } catch (final NoSuchPlanException ex) {
      return Optional.empty();
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

      final var record$ = getSimulationDatasetRecord(
          connection,
          simulation.id(),
          planRevision,
          MODEL_REVISION,
          simulation.revision()
      );
      if (record$.isEmpty()) return;

      deleteDataset(connection, record$.get().datasetID());
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

  private static Optional<SimulationDatasetRecord> getSimulationDatasetRecord(
      final Connection connection,
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision
  ) throws SQLException
  {
    try (final var getSimulationDatasetAction = new GetSimulationDatasetAction(connection)) {
      return getSimulationDatasetAction
          .get(simulationId, modelRevision, planRevision, simulationRevision);
    }
  }

  private static SimulationRecord createSimulation(
      final Connection connection,
      final long planId,
      final Map<String, SerializedValue> arguments
  ) throws SQLException
  {
    try (final var createSimulationAction = new CreateSimulationAction(connection)) {
      return createSimulationAction.apply(planId, arguments);
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
    final var dataset = createDataset(
        connection,
        simulation.planId(),
        planStart,
        simulationStart);
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
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var createDatasetAction = new CreateDatasetAction(connection)) {
      return createDatasetAction.apply(
          planId,
          planStart,
          simulationStart);
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
          simulationRevision,
          new State.Incomplete());
    }
  }

  private static void cancelStaleDatasets(
      final Connection connection,
      final long simulationId
  ) throws SQLException
  {
    try (final var cancelSimulationsAction = new CancelOutdatedSimulationsAction(connection)) {
      cancelSimulationsAction.apply(simulationId);
    }
  }

  private static void cancelSimulation(
      final Connection connection,
      final long datasetId
  ) throws SQLException, NoSuchSimulationDatasetException
  {
    try (final var cancelSimulationAction = new CancelSimulationAction(connection)) {
      cancelSimulationAction.apply(datasetId);
    }
  }

  private static boolean deleteDataset(final Connection connection, final long datasetId) throws SQLException {
    try (
        final var deleteSimulationDatasetAction = new DeleteSimulationDatasetAction(connection);
        final var deleteDatasetAction = new DeleteDatasetAction(connection)
    ) {
      return deleteSimulationDatasetAction.apply(datasetId) &&
             deleteDatasetAction.apply(datasetId);
    }
  }

  private static void failSimulation(
      final Connection connection,
      final long datasetId,
      final String reason
  ) throws SQLException, NoSuchSimulationDatasetException
  {
    try (final var setSimulationStateAction = new SetSimulationStateAction(connection)) {
      setSimulationStateAction.apply(datasetId, new State.Failed(reason));
    }
  }

  private static Optional<State> getSimulationState(
      final Connection connection,
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision,
      final Timestamp planStart
  ) throws SQLException {
    final var record$ = getSimulationDatasetRecord(
        connection,
        simulationId,
        modelRevision,
        planRevision,
        simulationRevision);
    if (record$.isEmpty()) return Optional.empty();
    final var record = record$.get();

    return Optional.of(
        switch (record.state()) {
          case "incomplete" -> new ResultsProtocol.State.Incomplete();
          case "failed" -> new ResultsProtocol.State.Failed(record.reason());
          case "success" -> {
            final var dataset = getDataset(connection, record.datasetID())
                .orElseThrow(() -> new Error("Simulation has \"success\" stat, but no valid dataset found"));
            yield new ResultsProtocol.State.Success(getSimulationResults(connection, dataset));
          }
          default -> throw new Error(String.format("Unexpected simulation state %s", record.state()));
        });
  }

  private static SimulationResults getSimulationResults(
      final Connection connection,
      final DatasetRecord dataset
  ) throws SQLException {
    final var simulationWindow = getSimulationWindow(connection, dataset);
    final var startTimestamp = simulationWindow.start();
    final var simulationStart = startTimestamp.toInstant();

    final var profiles = getProfiles(connection, dataset.id(), simulationWindow);
    final var realProfiles = profiles.getLeft();
    final var discreteProfiles = profiles.getRight();

    final var activities = getSimulatedActivities(connection, dataset.id(), startTimestamp);

    // TODO: Currently we don't store unfinished activities, but when we do we'll have to update this
    final Map<String, SerializedActivity> unfinishedActivities = Map.of();

    // TODO: Events are not currently persisted in the database. When they are, this stub will need to be updated.
    final var events = new ArrayList<Pair<Duration, EventGraph<Triple<String, ValueSchema, SerializedValue>>>>();

    return new SimulationResults(
        realProfiles,
        discreteProfiles,
        activities,
        unfinishedActivities,
        simulationStart,
        events
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

  private static Window getSimulationWindow(
      final Connection connection,
      final DatasetRecord dataset
  ) throws SQLException {
    try {
      final var plan = getPlan(connection, dataset.planId());
      final var simulationStart = plan.startTime()
          .plusMicros(dataset.offsetFromPlanStart().dividedBy(Duration.MICROSECONDS));
      final var simulationEnd = plan.endTime();
      return new Window(simulationStart, simulationEnd);
    } catch (final NoSuchPlanException ex) {
      throw new Error("Plan for simulation dataset with ID " + dataset.id() + " no longer exists.");
    }
  }

  private static PlanRecord getPlan(
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
      final Window simulationWindow
  ) throws SQLException {
    final var realProfiles = new HashMap<String, List<Pair<Duration, RealDynamics>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();

    final var profileRecords = getProfileRecords(connection, datasetId);
    for (final var record : profileRecords) {
      switch (record.type().getLeft()) {
        case "real" -> realProfiles.put(record.name(), getRealProfileSegments(connection, record.datasetId(), record.id(), simulationWindow));
        case "discrete" -> discreteProfiles.put(record.name(),
                                                Pair.of(
                                                    record.type().getRight(),
                                                    getDiscreteProfileSegments(connection, record.datasetId(), record.id(), simulationWindow)));
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
      final Window simulationWindow
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, simulationWindow, realDynamicsP);
    }
  }

  private static List<Pair<Duration, SerializedValue>> getDiscreteProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Window simulationWindow
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, simulationWindow, serializedValueP);
    }
  }

  private static void postSimulationResults(
      final Connection connection,
      final long datasetId,
      final SimulationResults results
  ) throws SQLException, NoSuchSimulationDatasetException
  {
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
    private final long simulationId;
    private final long datasetId;
    private final long modelRevision;
    private final long planRevision;
    private final long simulationRevision;
    private final Timestamp planStart;

    public PostgresResultsCell(
        final DataSource dataSource,
        final long simulationId,
        final long datasetId,
        final long modelRevision,
        final long planRevision,
        final long simulationRevision,
        final Timestamp planStart
    ) {
      this.dataSource = dataSource;
      this.simulationId = simulationId;
      this.datasetId = datasetId;
      this.modelRevision = modelRevision;
      this.planRevision = planRevision;
      this.simulationRevision = simulationRevision;
      this.planStart = planStart;
    }

    @Override
    public State get() {
      try (final var connection = dataSource.getConnection()) {
        return getSimulationState(
            connection,
            simulationId,
            modelRevision,
            planRevision,
            simulationRevision,
            planStart)
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
      } catch (final NoSuchSimulationDatasetException ex) {
        // A cell should only be created for a valid, existing dataset
        // A dataset should only be deleted by its cell
        throw new Error("Cell references nonexistent simulation dataset");
      }
    }

    @Override
    public boolean isCanceled() {
      try (final var connection = dataSource.getConnection()) {
        return getSimulationDatasetRecord(
            connection,
            simulationId,
            modelRevision,
            planRevision,
            simulationRevision
        ).map(SimulationDatasetRecord::canceled)
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
      } catch (final NoSuchSimulationDatasetException ex) {
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
      } catch (final NoSuchSimulationDatasetException ex) {
        // A cell should only be created for a valid, existing dataset
        // A dataset should only be deleted by its cell
        throw new Error("Cell references nonexistent simulation dataset");
      }
    }
  }
}
