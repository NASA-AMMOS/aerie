package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;

public final class PostgresResultsCellRepository implements ResultsCellRepository {
  private final DataSource dataSource;

  public PostgresResultsCellRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public ResultsProtocol.OwnerRole allocate(final PlanId planId) {
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

      final var dataset = createSimulationDataset(
          connection,
          simulation,
          planStart,
          simulationStart);

      return new PostgresResultsCell(
          this.dataSource,
          simulation,
          dataset.datasetId(),
          planStart);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to allocation simulation cell", ex);
    } catch (final NoSuchPlanException ex) {
      throw new Error("Cannot allocate simulation cell for nonexistent plan", ex);
    }
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final PlanId planId) {
    try (final var connection = this.dataSource.getConnection()) {
      final var planStart = getPlan(connection, planId).startTime();

      final var simulation$ = getSimulation(connection, planId);
      if (simulation$.isEmpty()) return Optional.empty();
      final var simulation = simulation$.get();

      final var datasetId$ = lookupSimulationDatasetRecord(
          connection,
          simulation.id(),
          planStart
      ).map(SimulationDatasetRecord::datasetId);

      if (datasetId$.isEmpty()) return Optional.empty();
      final var datasetId = datasetId$.get();

      return Optional.of(new PostgresResultsCell(this.dataSource,
                                                 simulation,
                                                 datasetId,
                                                 planStart));
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get simulation", ex);
    } catch (final NoSuchPlanException ex) {
      return Optional.empty();
    }
  }

  @Override
  public void deallocate(final ResultsProtocol.OwnerRole resultsCell) {
    if (!(resultsCell instanceof PostgresResultsCell cell)) {
      throw new Error("Unable to deallocate results cell of unknown type");
    }
    try (final var connection = this.dataSource.getConnection()) {
      deleteSimulationDataset(connection, cell.datasetId);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete simulation", ex);
    }
  }

  /* Database accessors */
  private static Optional<SimulationRecord> getSimulation(
      final Connection connection,
      final PlanId planId
  ) throws SQLException
  {
    try (final var getSimulationAction = new GetSimulationAction(connection)) {
      return getSimulationAction.get(planId);
    }
  }

  private static Optional<SimulationDatasetRecord> lookupSimulationDatasetRecord(
      final Connection connection,
      final long simulationId,
      final Timestamp planStart
  ) throws SQLException
  {
    try (final var lookupSimulationDatasetAction = new LookupSimulationDatasetAction(connection)) {
      return lookupSimulationDatasetAction.get(simulationId, planStart);
    }
  }

  private static Optional<SimulationDatasetRecord> getSimulationDatasetRecord(
      final Connection connection,
      final long datasetId,
      final Timestamp planStart
  ) throws SQLException
  {
    try (final var getSimulationDatasetAction = new GetSimulationDatasetAction(connection)) {
      return getSimulationDatasetAction.get(datasetId, planStart);
    }
  }

  private static SimulationRecord createSimulation(
      final Connection connection,
      final PlanId planId,
      final Map<String, SerializedValue> arguments
  ) throws SQLException
  {
    try (final var createSimulationAction = new CreateSimulationAction(connection)) {
      return createSimulationAction.apply(planId, arguments);
    }
  }

  private static SimulationDatasetRecord createSimulationDataset(
      final Connection connection,
      final SimulationRecord simulation,
      final Timestamp planStart,
      final Timestamp simulationStart
  ) throws SQLException
  {
    try (final var createSimulationDatasetAction = new CreateSimulationDatasetAction(connection)) {
      return createSimulationDatasetAction.apply(
          simulation.id(),
          planStart,
          simulationStart);
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

  private static boolean deleteSimulationDataset(final Connection connection, final long datasetId) throws SQLException {
    try (final var deleteSimulationDatasetAction = new DeleteSimulationDatasetAction(connection)) {
      return deleteSimulationDatasetAction.apply(datasetId);
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
      final long datasetId,
      final PlanId planId,
      final Timestamp planStart
  ) throws SQLException {
    final var record$ = getSimulationDatasetRecord(
        connection,
        datasetId,
        planStart);
    if (record$.isEmpty()) return Optional.empty();
    final var record = record$.get();

    return Optional.of(
        switch (record.state().state()) {
          case "incomplete" -> new ResultsProtocol.State.Incomplete();
          case "failed" -> new ResultsProtocol.State.Failed(record.state().reason());
          case "success" -> new ResultsProtocol.State.Success(getSimulationResults(connection, record, planId));
          default -> throw new Error(String.format("Unexpected simulation state %s", record.state()));
        });
  }

  private static SimulationResults getSimulationResults(
      final Connection connection,
      final SimulationDatasetRecord simulationDatasetRecord,
      final PlanId planId
  ) throws SQLException {
    final var simulationWindow = getSimulationWindow(connection, simulationDatasetRecord, planId);
    final var startTimestamp = simulationWindow.start();
    final var simulationStart = startTimestamp.toInstant();

    final var profiles = ProfileRepository.getProfiles(connection, simulationDatasetRecord.datasetId(), simulationWindow);
    final var activities = getSimulatedActivities(connection, simulationDatasetRecord.datasetId(), startTimestamp);

    // TODO: Currently we don't store unfinished activities, but when we do we'll have to update this
    final Map<ActivityInstanceId, SerializedActivity> unfinishedActivities = Map.of();

    final var topics = getSimulationTopics(connection, simulationDatasetRecord.datasetId());
    final var events = getSimulationEvents(connection, simulationDatasetRecord.datasetId(), startTimestamp);

    return new SimulationResults(
        profiles.realProfiles(),
        profiles.discreteProfiles(),
        activities,
        unfinishedActivities,
        simulationStart,
        topics,
        events
    );
  }

  private static List<Triple<Integer, String, ValueSchema>> getSimulationTopics(Connection connection, long datasetId)
  throws SQLException
  {
    try (final var getSimulationTopicsAction = new GetSimulationTopicsAction(connection)) {
      return getSimulationTopicsAction.get(datasetId);
    }
  }

  private static SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>>
  getSimulationEvents(
      final Connection connection,
      final long datasetId,
      final Timestamp startTime
  ) throws SQLException
  {
    try (final var getSimulationEventsAction = new GetSimulationEventsAction(connection)) {
      return getSimulationEventsAction.get(datasetId, startTime);
    }
  }

  private static Map<ActivityInstanceId, SimulatedActivity> getSimulatedActivities(
      final Connection connection,
      final long datasetId,
      final Timestamp startTime
  ) throws SQLException {
    try (final var getSimulatedActivitiesAction = new GetSimulatedActivitiesAction(connection)) {
      final var activityRecords = getSimulatedActivitiesAction.get(datasetId, startTime);
      final var pgIdToSimId = liftDirectiveIds(activityRecords);

      // Remap all activity IDs to reflect lifted directive IDs
      final var simulatedActivities = new HashMap<ActivityInstanceId, SimulatedActivity>(activityRecords.size());
      for (final var entry : activityRecords.entrySet()) {
        final var pgId = entry.getKey();
        final var record = entry.getValue();

        simulatedActivities.put(pgIdToSimId.get(pgId), new SimulatedActivity(
            record.type(),
            record.arguments(),
            record.start(),
            record.duration(),
            record.parentId().map(pgIdToSimId::get).orElse(null),
            record.childIds().stream().map(pgIdToSimId::get).collect(Collectors.toList()),
            record.directiveId(),
            record.computedAttributes()
        ));
      }

      return simulatedActivities;
    }
  }

  private static HashMap<Long, ActivityInstanceId> liftDirectiveIds(final Map<Long, SimulatedActivityRecord> activityRecords) {
    final var pgIdToSimId = new HashMap<Long, ActivityInstanceId>(activityRecords.size());
    final var simIds = new HashSet<Long>(activityRecords.size());

    for (final var id : activityRecords.keySet()) {
      final var record = activityRecords.get(id);
      if (record.directiveId().isEmpty()) continue;

      final var directiveId = record.directiveId().get();
      pgIdToSimId.put(id, directiveId);
      simIds.add(directiveId.id());
    }

    var counter = 1L;
    for (final var id : activityRecords.keySet()) {
      final var record = activityRecords.get(id);
      if (record.directiveId().isPresent()) continue;

      long newId;
      do {
        newId = counter++;
      } while (simIds.contains(newId));

      pgIdToSimId.put(id, new ActivityInstanceId(newId));
      simIds.add(newId);
    }
    return pgIdToSimId;
  }

  private static Window getSimulationWindow(
      final Connection connection,
      final SimulationDatasetRecord simulationDatasetRecord,
      final PlanId planId
  ) throws SQLException {
    try {
      final var plan = getPlan(connection, planId);
      final var simulationStart = plan.startTime()
          .plusMicros(simulationDatasetRecord.offsetFromPlanStart().dividedBy(Duration.MICROSECONDS));
      final var simulationEnd = plan.endTime();
      return new Window(simulationStart, simulationEnd);
    } catch (final NoSuchPlanException ex) {
      throw new Error("Plan for simulation dataset with ID " + simulationDatasetRecord.datasetId() + " no longer exists.");
    }
  }

  private static PlanRecord getPlan(
      final Connection connection,
      final PlanId planId
  ) throws SQLException, NoSuchPlanException {
    try (final var getPlanAction = new GetPlanAction(connection)) {
      return getPlanAction.get(planId);
    }
  }

  private static void postSimulationResults(
      final Connection connection,
      final long datasetId,
      final SimulationResults results
  ) throws SQLException, NoSuchSimulationDatasetException
  {
    final var simulationStart = new Timestamp(results.startTime);
    final var profileSet = ProfileSet.of(results.realProfiles, results.discreteProfiles);
    ProfileRepository.postResourceProfiles(connection, datasetId, profileSet, simulationStart);
    postSimulatedActivities(connection, datasetId, results.simulatedActivities, simulationStart);
    insertSimulationTopics(connection, datasetId, results.topics);
    insertSimulationEvents(connection, datasetId, results.events, simulationStart);

    try (final var setSimulationStateAction = new SetSimulationStateAction(connection)) {
      setSimulationStateAction.apply(datasetId, new State.Success(results));
    }
  }

  private static void insertSimulationTopics(
      Connection connection,
      long datasetId,
      final List<Triple<Integer, String, ValueSchema>> topics) throws SQLException
  {
    try (
        final var insertSimulationTopicsAction = new InsertSimulationTopicsAction(connection);
    ) {
      insertSimulationTopicsAction.apply(datasetId, topics);
    }
  }

  private static void insertSimulationEvents(
      Connection connection,
      long datasetId,
      Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events,
      Timestamp simulationStart) throws SQLException
  {
    try (
        final var insertSimulationEventsAction = new InsertSimulationEventsAction(connection);
    ) {
        insertSimulationEventsAction.apply(datasetId, events, simulationStart);
    }
  }

  private static void postSimulatedActivities(
      final Connection connection,
      final long datasetId,
      final Map<ActivityInstanceId, SimulatedActivity> simulatedActivities,
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

  public static final class PostgresResultsCell implements ResultsProtocol.OwnerRole {
    private final DataSource dataSource;
    private final SimulationRecord simulation;
    private final long datasetId;
    private final Timestamp planStart;

    public PostgresResultsCell(
        final DataSource dataSource,
        final SimulationRecord simulation,
        final long datasetId,
        final Timestamp planStart
    ) {
      this.dataSource = dataSource;
      this.simulation = simulation;
      this.datasetId = datasetId;
      this.planStart = planStart;
    }

    @Override
    public State get() {
      try (final var connection = dataSource.getConnection()) {
        return getSimulationState(
            connection,
            datasetId,
            simulation.planId(),
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
        return lookupSimulationDatasetRecord(
            connection,
            simulation.id(),
            planStart
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
