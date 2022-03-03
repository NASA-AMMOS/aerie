package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;
import gov.nasa.jpl.aerie.scheduler.server.services.SchedulingDSLCompilationService;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.stream.Collectors;

public final class PostgresSpecificationRepository implements SpecificationRepository {
  private final DataSource dataSource;
  private final SchedulingDSLCompilationService schedulingDSLCompilationService;

  public PostgresSpecificationRepository(final DataSource dataSource, final SchedulingDSLCompilationService schedulingDSLCompilationService) {
    this.dataSource = dataSource;
    this.schedulingDSLCompilationService = schedulingDSLCompilationService;
  }

  @Override
  public Specification getSpecification(final SpecificationId specificationId) throws NoSuchSpecificationException {
    try (final var connection = this.dataSource.getConnection()) {
      try (
          final var getSpecificationAction = new GetSpecificationAction(connection);
          final var getSpecificationGoalsAction = new GetSpecificationGoalsAction(connection)
      ) {
        final var specificationRecord = getSpecificationAction
            .get(specificationId.id())
            .orElseThrow(() -> new NoSuchSpecificationException(specificationId));
        final var planId = new PlanId(specificationRecord.planId());
        final var goals = getSpecificationGoalsAction
            .get(specificationId.id())
            .stream()
            .map((PostgresGoalRecord pgGoal) -> GoalBuilder.buildGoalRecord(
                planId,
                pgGoal,
                specificationRecord.horizonStartTimestamp(),
                specificationRecord.horizonEndTimestamp(),
                this.schedulingDSLCompilationService))
            .collect(Collectors.toList());

        return new Specification(
            planId,
            specificationRecord.planRevision(),
            goals,
            specificationRecord.horizonStartTimestamp(),
            specificationRecord.horizonEndTimestamp(),
            specificationRecord.simulationArguments()
        );
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get scheduling specification", ex);
    }
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
