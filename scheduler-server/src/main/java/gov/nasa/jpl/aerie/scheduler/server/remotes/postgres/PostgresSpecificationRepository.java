package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.SpecificationLoadException;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingCompilationError;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;
import gov.nasa.jpl.aerie.scheduler.server.services.SchedulingDSLCompilationService;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class PostgresSpecificationRepository implements SpecificationRepository {
  private final DataSource dataSource;
  private final SchedulingDSLCompilationService schedulingDSLCompilationService;

  public PostgresSpecificationRepository(final DataSource dataSource, final SchedulingDSLCompilationService schedulingDSLCompilationService) {
    this.dataSource = dataSource;
    this.schedulingDSLCompilationService = schedulingDSLCompilationService;
  }

  @Override
  public Specification getSpecification(final SpecificationId specificationId)
  throws NoSuchSpecificationException, SpecificationLoadException
  {
    final SpecificationRecord specificationRecord;
    final PlanId planId;
    final List<PostgresGoalRecord> postgresGoalRecords;
    try (final var connection = this.dataSource.getConnection();
         final var getSpecificationAction = new GetSpecificationAction(connection);
         final var getSpecificationGoalsAction = new GetSpecificationGoalsAction(connection)
    ) {
      specificationRecord = getSpecificationAction
          .get(specificationId.id())
          .orElseThrow(() -> new NoSuchSpecificationException(specificationId));
      planId = new PlanId(specificationRecord.planId());
      postgresGoalRecords = getSpecificationGoalsAction.get(specificationId.id());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get scheduling specification", ex);
    }

    final var goals = postgresGoalRecords
        .stream()
        .map((PostgresGoalRecord pgGoal) -> compileGoalDefinition(
            planId,
            pgGoal,
            this.schedulingDSLCompilationService))
        .toList();

    final var successfulGoals = new ArrayList<GoalRecord>();
    final var failedGoals = new ArrayList<GoalCompilationResult.Failure>();
    for (final var goalBuildResult : goals) {
      if (goalBuildResult instanceof GoalCompilationResult.Failure g) {
        failedGoals.add(g);
      } else if (goalBuildResult instanceof GoalCompilationResult.Success g) {
        successfulGoals.add(g.goalRecord());
      } else {
        throw new Error("Unhandled variant of GoalCompilationResult: " + goalBuildResult);
      }
    }

    if (!failedGoals.isEmpty()) {
      throw new SpecificationLoadException(specificationId,
                                           failedGoals
                                               .stream()
                                               .map(GoalCompilationResult.Failure::errors)
                                               .flatMap(List::stream)
                                               .toList()
                                               );
    }

    return new Specification(
        planId,
        specificationRecord.planRevision(),
        successfulGoals,
        specificationRecord.horizonStartTimestamp(),
        specificationRecord.horizonEndTimestamp(),
        specificationRecord.simulationArguments()
    );
  }

  private static GoalCompilationResult compileGoalDefinition(
      final PlanId planId,
      final PostgresGoalRecord pgGoal,
      final SchedulingDSLCompilationService schedulingDSLCompilationService)
  {
    final var goalCompilationResult = schedulingDSLCompilationService.compileSchedulingGoalDSL(
        planId,
        pgGoal.definition(),
        pgGoal.name());

    if (goalCompilationResult instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error g) {
      return new GoalCompilationResult.Failure(g.errors());
    } else if (goalCompilationResult instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success g) {
      return new GoalCompilationResult.Success(new GoalRecord(new GoalId(pgGoal.id()), g.goalSpecifier()));
    } else {
      throw new Error("Unhandled variant of SchedulingDSLCompilationResult: " + goalCompilationResult);
    }
  }

  private sealed interface GoalCompilationResult {
    record Success(GoalRecord goalRecord) implements GoalCompilationResult {}
    record Failure(List<SchedulingCompilationError.UserCodeError> errors) implements GoalCompilationResult {}
  }

  @Override
  public RevisionData getSpecificationRevisionData(final SpecificationId specificationId)
  throws NoSuchSpecificationException
  {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getSpecificationAction = new GetSpecificationAction(connection)) {
        final var specificationRevision = getSpecificationAction
            .get(specificationId.id())
            .orElseThrow(() -> new NoSuchSpecificationException(specificationId))
            .revision();

        return new SpecificationRevisionData(specificationRevision);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get scheduling specification revision data", ex);
    }
  }
}
