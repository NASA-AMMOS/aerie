package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchRequestException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleFailure;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults.GoalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PostgresResultsCellRepository implements ResultsCellRepository {
  private static final Logger logger = LoggerFactory.getLogger(PostgresResultsCellRepository.class);

  private final DataSource dataSource;

  public PostgresResultsCellRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public ResultsProtocol.OwnerRole allocate(final SpecificationId specificationId)
  {
    try (final var connection = this.dataSource.getConnection()) {
      final var spec = getSpecification(connection, specificationId);
      final var request = createRequest(connection, spec);

      return new PostgresResultsCell(
          this.dataSource,
          new SpecificationId(request.specificationId()),
          request.specificationRevision(),
          request.analysisId());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get schedule specification", ex);
    } catch (final NoSuchSpecificationException ex) {
      throw new Error("Cannot allocate scheduling cell for nonexistent specification", ex);
    }
  }

  @Override
  public Optional<ResultsProtocol.OwnerRole> claim(final SpecificationId specificationId)
  {
    try (
        final var connection = this.dataSource.getConnection();
        final var claimSimulationAction = new ClaimRequestAction(connection)
    ) {
      claimSimulationAction.apply(specificationId.id());

      final var spec = getSpecification(connection, specificationId);
      final var request$ = getRequest(connection, specificationId, spec.revision());
      if (request$.isEmpty()) return Optional.empty();
      final var request = request$.get();

      logger.info("Claimed scheduling request with specification id {}", specificationId);
      return Optional.of(new PostgresResultsCell(
          this.dataSource,
          new SpecificationId(request.specificationId()),
          request.specificationRevision(),
          request.analysisId()));
    } catch(UnclaimableRequestException ex) {
      return Optional.empty();
    } catch (final NoSuchSpecificationException ex) {
      throw new Error(String.format("Cannot process scheduling request for nonexistent specification %s%n", specificationId), ex);
    } catch (final SQLException | DatabaseException ex) {
      throw new Error(ex.getMessage());
    }
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final SpecificationId specificationId)
  {
    try (final var connection = this.dataSource.getConnection()) {
      final var spec = getSpecification(connection, specificationId);
      final var request$ = getRequest(connection, specificationId, spec.revision());
      if (request$.isEmpty()) return Optional.empty();
      final var request = request$.get();

      return Optional.of(new PostgresResultsCell(
          this.dataSource,
          new SpecificationId(request.specificationId()),
          request.specificationRevision(),
          request.analysisId()));
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get schedule specification", ex);
    } catch (final NoSuchSpecificationException ex) {
      return Optional.empty();
    }
  }

  @Override
  public void deallocate(final ResultsProtocol.OwnerRole resultsCell)
  {
    if (!(resultsCell instanceof PostgresResultsCell cell)) {
      throw new Error("Unable to deallocate results cell of unknown type");
    }
    try (final var connection = this.dataSource.getConnection()) {
      deleteRequest(connection, cell.specId, cell.specRevision);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete scheduling request", ex);
    }
  }

  private static SpecificationRecord getSpecification(final Connection connection, final SpecificationId specificationId)
  throws SQLException, NoSuchSpecificationException {
    try (final var getSpecificationAction = new GetSpecificationAction(connection)) {
      return getSpecificationAction
          .get(specificationId.id())
          .orElseThrow(() -> new NoSuchSpecificationException(specificationId));
    }
  }

  private static Optional<RequestRecord> getRequest(
      final Connection connection,
      final SpecificationId specificationId,
      final long specificationRevision
  ) throws SQLException {
    try (final var getRequestAction = new GetRequestAction(connection)) {
      return getRequestAction
          .get(specificationId.id(), specificationRevision);
    }
  }

  private static RequestRecord createRequest(
      final Connection connection,
      final SpecificationRecord specification
  ) throws SQLException {
    try (final var createRequestAction = new CreateRequestAction(connection)) {
      return createRequestAction.apply(specification);
    }
  }

  private static void deleteRequest(
      final Connection connection,
      final SpecificationId specId,
      final long specRevision
  ) throws SQLException {
    try (final var deleteRequestAction = new DeleteRequestAction(connection)) {
      deleteRequestAction.apply(specId.id(), specRevision);
    }
  }

  private static void cancelRequest(
      final Connection connection,
      final SpecificationId specId,
      final long specRevision
  ) throws SQLException, NoSuchRequestException
  {
    try (final var cancelSchedulingRequestAction = new CancelSchedulingRequestAction(connection)) {
      cancelSchedulingRequestAction.apply(specId.id(), specRevision);
    } catch (final FailedUpdateException ex) {
      throw new NoSuchRequestException(specId, specRevision);
    }
  }

  private static void failRequest(
       final Connection connection,
       final SpecificationId specId,
       final long specRevision,
       final ScheduleFailure reason
  ) throws SQLException {
    try (final var setRequestStateAction = new SetRequestStateAction(connection)) {
      setRequestStateAction.apply(
          specId.id(),
          specRevision,
          RequestRecord.Status.FAILED,
          reason);
    }
  }

  private static void succeedRequest(
      final Connection connection,
      final SpecificationId specId,
      final long specRevision,
      final long analysisId,
      final ScheduleResults results
  ) throws SQLException {
    postResults(connection, analysisId, results);
    try (final var setRequestStateAction = new SetRequestStateAction(connection)) {
      setRequestStateAction.apply(
          specId.id(),
          specRevision,
          RequestRecord.Status.SUCCESS,
          null);
    }
  }

  private static void postResults(
      final Connection connection,
      final long analysisId,
      final ScheduleResults results
  ) throws SQLException {
    final var numGoals = results.goalResults().size();
    final var goalSatisfaction = new HashMap<GoalId, Boolean>(numGoals);
    final var createdActivities = new HashMap<GoalId, Collection<ActivityDirectiveId>>(numGoals);
    final var satisfyingActivities = new HashMap<GoalId, Collection<ActivityDirectiveId>>(numGoals);

    results.goalResults().forEach((goalId, result) -> {
      goalSatisfaction.put(goalId, result.satisfied());
      createdActivities.put(goalId, result.createdActivities());
      satisfyingActivities.put(goalId, result.satisfyingActivities());
    });

    try (
        final var insertGoalSatisfactionAction = new InsertGoalSatisfactionAction(connection);
        final var insertCreatedActivitiesAction = new InsertCreatedActivitiesAction(connection);
        final var insertSatisfyingActivitiesAction = new InsertSatisfyingActivitiesAction(connection)
    ) {
      insertGoalSatisfactionAction.apply(analysisId, goalSatisfaction);
      insertCreatedActivitiesAction.apply(analysisId, createdActivities);
      insertSatisfyingActivitiesAction.apply(analysisId, satisfyingActivities);
    }
  }

  private static ScheduleResults getResults(
      final Connection connection,
      final long analysisId
  ) throws SQLException {
    try (
        final var getGoalSatisfactionAction = new GetGoalSatisfactionAction(connection);
        final var getCreatedActivitiesAction = new GetCreatedActivitiesAction(connection);
        final var getSatisfyingActivitiesAction = new GetSatisfyingActivitiesAction(connection)
    ) {
      final var goalSatisfaction = getGoalSatisfactionAction.get(analysisId);
      final var createdActivities = getCreatedActivitiesAction.get(analysisId);
      final var satisfyingActivities = getSatisfyingActivitiesAction.get(analysisId);

      final var goalResults = new HashMap<GoalId, GoalResult>(goalSatisfaction.size());
      for (final var goalId : goalSatisfaction.keySet()) {
        goalResults.put(goalId, new GoalResult(
            createdActivities.get(goalId),
            satisfyingActivities.get(goalId),
            goalSatisfaction.get(goalId)
        ));
      }

      return new ScheduleResults(goalResults);
    }
  }

  private static Optional<ResultsProtocol.State> getRequestState(
      final Connection connection,
      final SpecificationId specId,
    final long specRevision
  ) throws SQLException {
    final var request$ = getRequest(connection, specId, specRevision);
    if (request$.isEmpty()) return Optional.empty();
    final var request = request$.get();

    return Optional.of(
        switch (request.status()) {
          case PENDING -> new ResultsProtocol.State.Pending(request.analysisId());
          case INCOMPLETE -> new ResultsProtocol.State.Incomplete(request.analysisId());
          case FAILED -> new ResultsProtocol.State.Failed(
              request.reason()
                  .orElseThrow(() -> new Error("Unexpected state: %s request state has no failure message".formatted(request.status()))),
              request.analysisId());
          case SUCCESS -> new ResultsProtocol.State.Success(getResults(connection, request.analysisId()), request.analysisId());
        }
    );
  }

  public static final class PostgresResultsCell implements ResultsProtocol.OwnerRole {
    private final DataSource dataSource;
    private final SpecificationId specId;
    private final long specRevision;
    private final long analysisId;

    public PostgresResultsCell(
        final DataSource dataSource,
        final SpecificationId specId,
        final long specRevision,
        final long analysisId
    ) {
      this.dataSource = dataSource;
      this.specId = specId;
      this.specRevision = specRevision;
      this.analysisId = analysisId;
    }

    @Override
    public ResultsProtocol.State get() {
      try (final var connection = dataSource.getConnection()) {
        return getRequestState(
            connection,
            specId,
            specRevision)
            .orElseThrow(() -> new Error("Scheduling request no longer exists"));
      } catch(final SQLException ex) {
        throw new DatabaseException("Failed to get scheduling request status", ex);
      }
    }

    @Override
    public void cancel() {
      try (final var connection = dataSource.getConnection()) {
        cancelRequest(connection, specId, specRevision);
      } catch (final NoSuchRequestException ex) {
        throw new Error("Scheduling request no longer exists");
      } catch(final SQLException ex) {
        throw new DatabaseException("Failed to cancel scheduling request", ex);
      }
    }

    @Override
    public boolean isCanceled() {
      try (final var connection = dataSource.getConnection()) {
        return getRequest(connection, specId, specRevision)
            .map(RequestRecord::canceled)
            .orElseThrow(() -> new Error("Scheduling request no longer exists"));
      } catch(final SQLException ex) {
        throw new DatabaseException("Failed to determine if scheduling request is canceled", ex);
      }
    }

    @Override
    public void succeedWith(final ScheduleResults results) {
      try (final var connection = dataSource.getConnection()) {
        succeedRequest(connection, specId, specRevision, analysisId, results);
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to update scheduling request state", ex);
      }
    }

    @Override
    public void failWith(final ScheduleFailure reason) {
      try (final var connection = dataSource.getConnection()) {
        failRequest(connection, specId, specRevision, reason);
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to update scheduling request state", ex);
      }
    }
  }
}
