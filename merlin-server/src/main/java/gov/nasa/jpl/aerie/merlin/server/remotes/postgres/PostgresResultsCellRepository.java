package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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
      final SimulationRecord simulation = getSimulation(connection, planId);
      final SimulationTemplateRecord template;
      final Timestamp startTime = simulation.simulationStartTime();
      final Timestamp endTime = simulation.simulationEndTime();
      final var arguments = new HashMap<String, SerializedValue>();

      if (simulation.simulationTemplateId().isPresent()) {
          try (final var getSimulationTemplate = new GetSimulationTemplateAction(connection)) {
            final var templateOptional = getSimulationTemplate.get(simulation.simulationTemplateId().get());
            if (templateOptional.isEmpty()) {
              throw new RuntimeException("TemplateRecord should not be empty");
            }
            template = templateOptional.get();
            arguments.putAll(template.arguments());
          }
      }
      if (startTime == null || endTime == null) {
        throw new RuntimeException("Simulation bounds are not fully defined. Unable to simulate.");
      }

      arguments.putAll(simulation.arguments());

      final var dataset = createSimulationDataset(
          connection,
          simulation,
          startTime,
          endTime,
          arguments);

      return new PostgresResultsCell(
          this.dataSource,
          simulation,
          dataset.datasetId());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to allocation simulation cell", ex);
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
      logger.info("Claimed simulation with dataset id {}", datasetId);

      final var simulation = getSimulation(connection, planId);

      return Optional.of(new PostgresResultsCell(
          this.dataSource,
          simulation,
          datasetId));
    } catch(UnclaimableSimulationException ex) {
      return Optional.empty();
    } catch(final SQLException | DatabaseException ex) {
      throw new Error(ex.getMessage());
    }
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final PlanId planId) {
    try (final var connection = this.dataSource.getConnection()) {
      final var simulation = getSimulation(connection, planId);
      final var datasetRecord = lookupSimulationDatasetRecord(
          connection,
          simulation.id());
      final var datasetId$ = datasetRecord.map(SimulationDatasetRecord::datasetId);

      if (datasetId$.isEmpty()) return Optional.empty();

      final var datasetId = datasetId$.get();
      return Optional.of(new PostgresResultsCell(this.dataSource, simulation, datasetId));
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get simulation", ex);
    }
  }

  /* Database accessors */
  private static SimulationRecord getSimulation(
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
      final long simulationId
  ) throws SQLException
  {
    try (final var lookupSimulationDatasetAction = new LookupSimulationDatasetAction(connection)) {
      return lookupSimulationDatasetAction.get(simulationId);
    }
  }

  private static Optional<SimulationDatasetRecord> getSimulationDatasetRecord(
      final Connection connection,
      final long datasetId
  ) throws SQLException
  {
    try (final var getSimulationDatasetAction = new GetSimulationDatasetAction(connection)) {
      return getSimulationDatasetAction.get(datasetId);
    }
  }

  private static SimulationDatasetRecord createSimulationDataset(
      final Connection connection,
      final SimulationRecord simulation,
      final Timestamp simulationStart,
      final Timestamp simulationEnd,
      final Map<String, SerializedValue> arguments
  ) throws SQLException
  {
    try (final var createSimulationDatasetAction = new CreateSimulationDatasetAction(connection)) {
      return createSimulationDatasetAction.apply(
          simulation.id(),
          simulationStart,
          simulationEnd,
          arguments);
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

  private static Optional<State> getSimulationState (final Connection connection, final long datasetId) throws SQLException {
    final var record$ = getSimulationDatasetRecord(connection, datasetId);
    if (record$.isEmpty()) return Optional.empty();
    final var record = record$.get();

    return Optional.of(
        switch (record.state().status()) {
          case PENDING -> new ResultsProtocol.State.Pending(record.simulationDatasetId());
          case INCOMPLETE -> new ResultsProtocol.State.Incomplete(record.simulationDatasetId());
          case FAILED -> new ResultsProtocol.State.Failed(record.simulationDatasetId(), record.state().reason()
              .orElseThrow(() -> new Error("Unexpected state: %s request state has no failure message".formatted(record.state().status()))));
          case SUCCESS -> new ResultsProtocol.State.Success(record.simulationDatasetId(), getSimulationResults(connection, record));
        });
  }

  private static SimulationResults getSimulationResults(
      final Connection connection,
      final SimulationDatasetRecord simulationDatasetRecord
  ) throws SQLException {
    final var startTimestamp = simulationDatasetRecord.simulationStartTime();
    final var endTimestamp = simulationDatasetRecord.simulationEndTime();

    final var simulationStart = startTimestamp.toInstant();
    final var duration = Duration.of(simulationStart.until(endTimestamp.toInstant(), ChronoUnit.MICROS), Duration.MICROSECONDS);
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
        duration,
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

  private static void postSimulationResults(
      final Connection connection,
      final long datasetId,
      final SimulationResultsInterface results
  ) throws SQLException, NoSuchSimulationDatasetException
  {
    final var simulationStart = new Timestamp(results.getStartTime());
    final var profileSet = ProfileSet.of(results.getRealProfiles(), results.getDiscreteProfiles());
    ProfileRepository.postResourceProfiles(connection, datasetId, profileSet);
    postActivities(connection, datasetId,
                   results.getSimulatedActivities(),
                   results.getUnfinishedActivities(), simulationStart);
    insertSimulationTopics(connection, datasetId, results.getTopics());
    insertSimulationEvents(connection, datasetId, results.getEvents(), simulationStart);

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
        final var insertSimulationTopicsAction = new InsertSimulationTopicsAction(connection)
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
        final var insertSimulationEventsAction = new InsertSimulationEventsAction(connection)
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
    private final long datasetId;

    public PostgresResultsCell(
        final DataSource dataSource,
        final SimulationRecord simulation,
        final long datasetId
    ) {
      this.dataSource = dataSource;
      this.simulation = simulation;
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
            simulation.id())
         .map(SimulationDatasetRecord::canceled)
         .orElseThrow(() -> new Error("Dataset corrupted"));
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to check cancellation status", ex);
      }
    }

    @Override
    public void succeedWith(final SimulationResultsInterface results) {
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
