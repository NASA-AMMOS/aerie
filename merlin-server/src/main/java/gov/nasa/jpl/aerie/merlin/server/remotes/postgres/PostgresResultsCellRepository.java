package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

      cancelStaleSimulationDatasets(connection, simulation.id());

      final var dataset = createSimulationDataset(
          connection,
          simulation,
          MODEL_REVISION,
          planRevision,
          planStart,
          simulationStart);

      return new PostgresResultsCell(
          this.dataSource,
          simulation,
          dataset.datasetID(),
          MODEL_REVISION,
          planRevision,
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
          simulation.revision(),
          planStart
      ).map(SimulationDatasetRecord::datasetID);

      if (datasetId$.isEmpty()) return Optional.empty();
      final var datasetId = datasetId$.get();

      return Optional.of(new PostgresResultsCell(this.dataSource,
                                                 simulation,
                                                 datasetId,
                                                 MODEL_REVISION,
                                                 planRevision,
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
      final var planStart = getPlan(connection, planId).startTime();
      final var simulation$ = getSimulation(connection, planId);
      if (simulation$.isEmpty()) return;
      final var simulation = simulation$.get();

      final var record$ = getSimulationDatasetRecord(
          connection,
          simulation.id(),
          MODEL_REVISION,
          planRevision,
          simulation.revision(),
          planStart
      );
      if (record$.isEmpty()) return;

      deleteSimulationDataset(connection, record$.get().datasetID());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete simulation", ex);
    } catch (final NoSuchPlanException ex) {
      throw new Error("Deallocation of results cell not possible, plan was removed");
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

  private static Optional<SimulationDatasetRecord> getSimulationDatasetRecord(
      final Connection connection,
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision,
      final Timestamp planStart
  ) throws SQLException
  {
    try (final var getSimulationDatasetAction = new GetSimulationDatasetAction(connection)) {
      return getSimulationDatasetAction
          .get(simulationId, modelRevision, planRevision, simulationRevision, planStart);
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

  private static SimulationDatasetRecord createSimulationDataset(
      final Connection connection,
      final SimulationRecord simulation,
      final long modelRevision,
      final long planRevision,
      final Timestamp planStart,
      final Timestamp simulationStart
  ) throws SQLException
  {
    try (final var createSimulationDatasetAction = new CreateSimulationDatasetAction(connection)) {
      return createSimulationDatasetAction.apply(
          simulation.id(),
          modelRevision,
          planRevision,
          simulation.revision(),
          planStart,
          simulationStart,
          new ResultsProtocol.State.Incomplete());
    }
  }

  private static void cancelStaleSimulationDatasets(
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
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision,
      final long planId,
      final Timestamp planStart
  ) throws SQLException {
    final var record$ = getSimulationDatasetRecord(
        connection,
        simulationId,
        modelRevision,
        planRevision,
        simulationRevision,
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
      final long planId
  ) throws SQLException {
    final var simulationWindow = getSimulationWindow(connection, simulationDatasetRecord, planId);
    final var startTimestamp = simulationWindow.start();
    final var simulationStart = startTimestamp.toInstant();

    final var profiles = ProfileRepository.getProfiles(connection, simulationDatasetRecord.datasetID(), simulationWindow);
    final var realProfiles = profiles.getLeft();
    final var discreteProfiles = profiles.getRight();

    final var activities = getSimulatedActivities(connection, simulationDatasetRecord.datasetID(), startTimestamp);

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
      final SimulationDatasetRecord simulationDatasetRecord,
      final long planId
  ) throws SQLException {
    try {
      final var plan = getPlan(connection, planId);
      final var simulationStart = plan.startTime()
          .plusMicros(simulationDatasetRecord.offsetFromPlanStart().dividedBy(Duration.MICROSECONDS));
      final var simulationEnd = plan.endTime();
      return new Window(simulationStart, simulationEnd);
    } catch (final NoSuchPlanException ex) {
      throw new Error("Plan for simulation dataset with ID " + simulationDatasetRecord.datasetID() + " no longer exists.");
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

  private static void postSimulationResults(
      final Connection connection,
      final long datasetId,
      final SimulationResults results
  ) throws SQLException, NoSuchSimulationDatasetException
  {
    final var simulationStart = new Timestamp(results.startTime);
    ProfileRepository.postResourceProfiles(connection, datasetId, results.realProfiles, results.discreteProfiles, simulationStart);
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

  public static final class PostgresResultsCell implements ResultsProtocol.OwnerRole {
    private final DataSource dataSource;
    private final SimulationRecord simulation;
    private final long datasetId;
    private final long modelRevision;
    private final long planRevision;
    private final Timestamp planStart;

    public PostgresResultsCell(
        final DataSource dataSource,
        final SimulationRecord simulation,
        final long datasetId,
        final long modelRevision,
        final long planRevision,
        final Timestamp planStart
    ) {
      this.dataSource = dataSource;
      this.simulation = simulation;
      this.datasetId = datasetId;
      this.modelRevision = modelRevision;
      this.planRevision = planRevision;
      this.planStart = planStart;
    }

    @Override
    public State get() {
      try (final var connection = dataSource.getConnection()) {
        return getSimulationState(
            connection,
            simulation.id(),
            modelRevision,
            planRevision,
            simulation.revision(),
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
        return getSimulationDatasetRecord(
            connection,
            simulation.id(),
            modelRevision,
            planRevision,
            simulation.revision(),
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
