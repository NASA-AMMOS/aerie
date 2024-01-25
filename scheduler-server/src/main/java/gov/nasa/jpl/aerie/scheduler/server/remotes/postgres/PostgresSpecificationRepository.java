package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingConditionRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public final class PostgresSpecificationRepository implements SpecificationRepository {
  private final DataSource dataSource;

  public PostgresSpecificationRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Specification getSpecification(final SpecificationId specificationId)
  throws NoSuchSpecificationException
  {
    final SpecificationRecord specificationRecord;
    final PlanId planId;
    final List<GoalRecord> goals;
    final List<SchedulingConditionRecord> schedulingConditions;
    try (final var connection = this.dataSource.getConnection();
         final var getSpecificationAction = new GetSpecificationAction(connection);
         final var getSpecificationGoalsAction = new GetSpecificationGoalsAction(connection);
         final var getSpecificationConditionsAction = new GetSpecificationConditionsAction(connection)
    ) {
      specificationRecord = getSpecificationAction
          .get(specificationId.id())
          .orElseThrow(() -> new NoSuchSpecificationException(specificationId));
      planId = new PlanId(specificationRecord.planId());
      goals = getSpecificationGoalsAction.get(specificationId.id());
      schedulingConditions = getSpecificationConditionsAction.get(specificationId.id());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get scheduling specification", ex);
    }

    return new Specification(
        specificationId,
        specificationRecord.revision(),
        planId,
        specificationRecord.planRevision(),
        specificationRecord.horizonStartTimestamp(),
        specificationRecord.horizonEndTimestamp(),
        specificationRecord.simulationArguments(),
        specificationRecord.analysisOnly(),
        goals,
        schedulingConditions
    );
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
