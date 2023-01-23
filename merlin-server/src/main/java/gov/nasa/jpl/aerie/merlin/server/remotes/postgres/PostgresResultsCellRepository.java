package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationFailure;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.UnfinishedActivity;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;

public final class PostgresResultsCellRepository implements ResultsCellRepository {
  private static final Logger logger = LoggerFactory.getLogger(PostgresResultsCellRepository.class);

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

  /**
   * Claim a simulation
   *
   * <p>
   * For the case where the return value is empty, the simulation is already claimed. All other exceptions are
   * unexpected and result in an {@link Error} being raised.
   * </p>
   *
   * @param planId a plan identifier
   * @param datasetId the identifier of a dataset record
   * @return cell (handle) {@link ResultsProtocol.OwnerRole} to the claimed simulation wrapped in {@link Optional}
   */
  @Override
  public Optional<ResultsProtocol.OwnerRole> claim(final PlanId planId, final Long datasetId) {
    try (final var connection = this.dataSource.getConnection()) {
      claimSimulationDataset(connection, datasetId);
      logger.info("Claimed simulation with datatset id {}", datasetId);

      final var planStart = getPlan(connection, planId).startTime();

      final var simulation$ = getSimulation(connection, planId);
      if (simulation$.isEmpty()) {
        return Optional.empty();
      }
      final var simulation = simulation$.get();

      return Optional.of(new PostgresResultsCell(
          this.dataSource,
          simulation,
          datasetId,
          planStart));
    } catch(UnclaimableSimulationException ex) {
      return Optional.empty();
    } catch (final NoSuchPlanException ex) {
      throw new Error(String.format("Cannot process simulation for nonexistent plan %s%n", planId), ex);
    } catch(final SQLException | DatabaseException ex) {
      throw new Error(ex.getMessage());
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
      return getSimulationAction.get(planId.id());
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
      return createSimulationAction.apply(planId.id(), arguments);
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
  /**
   * Claim a simulation dataset, throwing an {@link UnclaimableSimulationException} if the dataset is already claimed.
   *
   * <p>
   *   The method can be unsuccessful in claiming a simulation in two ways. The first is that an
   *   {@link UnclaimableSimulationException} is thrown if the simulation is already claimed. The second is that
   *   there has been an SQL error resulting in {@link DatabaseException}.
   * </p>
   *
   * @param  connection an SQL database connection
   * @param  datasetId the identifier of a dataset record
   */
  private static void claimSimulationDataset(
      final Connection connection,
      final long datasetId
  ) throws SQLException, UnclaimableSimulationException
  {
    try (final var claimSimulationAction = new ClaimSimulationAction(connection)) {
        claimSimulationAction.apply(datasetId);
    }

    try (final var transactionContext = new TransactionContext(connection)) {
      final var createSimulationDatasetPartitionsAction = new CreateProfileSegmentPartitionAction(connection);
      final var createSpanPartitionAction = new CreateSpanPartitionAction(connection);
      final var createEventPartitionAction = new CreateEventPartitionAction(connection);

      createSimulationDatasetPartitionsAction.apply(datasetId);
      createSpanPartitionAction.apply(datasetId);
      createEventPartitionAction.apply(datasetId);
      transactionContext.commit();
    } catch (final SQLException ex) {
      throw new DatabaseException(String.format("Failed to create partitions for simulation dataset id %s", datasetId), ex);
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
      final SimulationFailure reason
  ) throws SQLException, NoSuchSimulationDatasetException
  {
    try (final var setSimulationStateAction = new SetSimulationStateAction(connection)) {
      setSimulationStateAction.apply(datasetId, SimulationStateRecord.failed(reason));
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
        switch (record.state().status()) {
          case PENDING -> new ResultsProtocol.State.Pending(record.simulationDatasetId());
          case INCOMPLETE -> new ResultsProtocol.State.Incomplete(record.simulationDatasetId());
          case FAILED -> new ResultsProtocol.State.Failed(record.simulationDatasetId(), record.state().reason()
              .orElseThrow(() -> new Error("Unexpected state: %s request state has no failure message".formatted(record.state().status()))));
          case SUCCESS -> new ResultsProtocol.State.Success(record.simulationDatasetId(), getSimulationResults(connection, record, planId));
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

    final var profiles = ProfileRepository.getProfiles(connection, simulationDatasetRecord.datasetId());
    final var activities = getActivities(connection, simulationDatasetRecord.datasetId(), startTimestamp);
    final var topics = getSimulationTopics(connection, simulationDatasetRecord.datasetId());
    final var events = getSimulationEvents(connection, simulationDatasetRecord.datasetId(), startTimestamp);

    return new SimulationResults(
        ProfileSet.unwrapOptional(profiles.realProfiles()),
        ProfileSet.unwrapOptional(profiles.discreteProfiles()),
        activities.getLeft(),
        activities.getRight(),
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

  private static Pair<Map<SimulatedActivityId, SimulatedActivity>, Map<SimulatedActivityId, UnfinishedActivity>> getActivities(
      final Connection connection,
      final long datasetId,
      final Timestamp startTime
  ) throws SQLException
  {
    try (final var getActivitiesAction = new GetSpanRecords(connection)) {
      final var activityRecords = getActivitiesAction.get(datasetId, startTime);

      // Remap all activity IDs to reflect lifted directive IDs
      final var simulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>();
      final var unfinishedActivities = new HashMap<SimulatedActivityId, UnfinishedActivity>();
      for (final var entry : activityRecords.entrySet()) {
        final var pgId = entry.getKey();
        final var record = entry.getValue();
        final var activityInstanceId = new SimulatedActivityId(pgId);

        // Only records with duration and computed attributes represent simulated activities
        if (record.duration().isPresent() && record.attributes().computedAttributes().isPresent()) {
          simulatedActivities.put(activityInstanceId, new SimulatedActivity(
              record.type(),
              record.attributes().arguments(),
              record.start(),
              record.duration().get(),
              record.parentId().map(SimulatedActivityId::new).orElse(null),
              record.childIds().stream().map(SimulatedActivityId::new).collect(Collectors.toList()),
              record.attributes().directiveId().map(ActivityDirectiveId::new),
              record.attributes().computedAttributes().get()
          ));
        } else {
          unfinishedActivities.put(activityInstanceId, new UnfinishedActivity(
              record.type(),
              record.attributes().arguments(),
              record.start(),
              record.parentId().map(SimulatedActivityId::new).orElse(null),
              record.childIds().stream().map(SimulatedActivityId::new).collect(Collectors.toList()),
              record.attributes().directiveId().map(ActivityDirectiveId::new)
          ));
        }
      }

      return Pair.of(simulatedActivities, unfinishedActivities);
    }
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
      return getPlanAction
          .get(planId.id())
          .orElseThrow(() -> new NoSuchPlanException(planId));
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
    ProfileRepository.postResourceProfiles(connection, datasetId, profileSet);
    postActivities(connection, datasetId, results.simulatedActivities, results.unfinishedActivities, simulationStart);
    insertSimulationTopics(connection, datasetId, results.topics);
    insertSimulationEvents(connection, datasetId, results.events, simulationStart);

    try (final var setSimulationStateAction = new SetSimulationStateAction(connection)) {
      setSimulationStateAction.apply(datasetId, SimulationStateRecord.success());
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

  private static void postActivities(
      final Connection connection,
      final long datasetId,
      final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities,
      final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
      final Timestamp simulationStart
  ) throws SQLException {
    try (
        final var postActivitiesAction = new PostSpansAction(connection);
        final var updateSimulatedActivityParentsAction = new UpdateSimulatedActivityParentsAction(connection)
    ) {
      final var simulatedActivityRecords = simulatedActivities.entrySet().stream()
          .collect(Collectors.toMap(
              e -> e.getKey().id(),
              e -> simulatedActivityToRecord(e.getValue())));

      final var allActivityRecords = unfinishedActivities.entrySet().stream()
          .collect(Collectors.toMap(
              e -> e.getKey().id(),
              e -> unfinishedActivityToRecord(e.getValue())));
      allActivityRecords.putAll(simulatedActivityRecords);

      final var simIdToPgId = postActivitiesAction.apply(
          datasetId,
          allActivityRecords,
          simulationStart);

      updateSimulatedActivityParentsAction.apply(
          datasetId,
          simulatedActivityRecords,
          simIdToPgId);
    }
  }

  private static SpanRecord simulatedActivityToRecord(final SimulatedActivity activity) {
    return new SpanRecord(
        activity.type(),
        activity.start(),
        Optional.of(activity.duration()),
        Optional.ofNullable(activity.parentId()).map(SimulatedActivityId::id),
        activity.childIds().stream().map(SimulatedActivityId::id).collect(Collectors.toList()),
        new ActivityAttributesRecord(
          activity.directiveId().map(ActivityDirectiveId::id),
          activity.arguments(),
          Optional.of(activity.computedAttributes())));
  }

  private static SpanRecord unfinishedActivityToRecord(final UnfinishedActivity activity) {
    return new SpanRecord(
        activity.type(),
        activity.start(),
        Optional.empty(),
        Optional.ofNullable(activity.parentId()).map(SimulatedActivityId::id),
        activity.childIds().stream().map(SimulatedActivityId::id).collect(Collectors.toList()),
        new ActivityAttributesRecord(
            activity.directiveId().map(ActivityDirectiveId::id),
            activity.arguments(),
            Optional.empty()));
  }

  public static final class PostgresResultsCell implements ResultsProtocol.OwnerRole {
    private final DataSource dataSource;
    private final SimulationRecord simulation;
    private final PlanId planId;
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
      this.planId = new PlanId(simulation.planId());
      this.datasetId = datasetId;
      this.planStart = planStart;
    }

    @Override
    public State get() {
      try (final var connection = dataSource.getConnection()) {
        return getSimulationState(
            connection,
            datasetId,
            planId,
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
      try (final var connection = dataSource.getConnection();
           final var transactionContext = new TransactionContext(connection)) {
        postSimulationResults(connection, datasetId, results);
        transactionContext.commit();
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to store simulation results", ex);
      } catch (final NoSuchSimulationDatasetException ex) {
        // A cell should only be created for a valid, existing dataset
        // A dataset should only be deleted by its cell
        throw new Error("Cell references nonexistent simulation dataset");
      }
    }

    @Override
    public void failWith(final SimulationFailure reason) {
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
