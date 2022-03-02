package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.ActivityType;
import gov.nasa.jpl.aerie.scheduler.CompositeAndGoal;
import gov.nasa.jpl.aerie.scheduler.Goal;
import gov.nasa.jpl.aerie.scheduler.OptionGoal;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;
import gov.nasa.jpl.aerie.scheduler.server.services.SchedulingGoalDSLCompilationService;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

public final class PostgresSpecificationRepository implements SpecificationRepository {
  private final DataSource dataSource;
  private final SchedulingGoalDSLCompilationService schedulingGoalDSLCompilationService;

  public PostgresSpecificationRepository(final DataSource dataSource, final SchedulingGoalDSLCompilationService schedulingGoalDSLCompilationService) {
    this.dataSource = dataSource;
    this.schedulingGoalDSLCompilationService = schedulingGoalDSLCompilationService;
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
        final var goals = getSpecificationGoalsAction
            .get(specificationId.id())
            .stream()
            .map((PostgresGoalRecord pgGoal) -> buildGoalRecord(pgGoal, specificationRecord.horizonStartTimestamp(), specificationRecord.horizonEndTimestamp(), this.schedulingGoalDSLCompilationService))
            .collect(Collectors.toList());

        return new Specification(
            new PlanId(specificationRecord.planId()),
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

  private static GoalRecord buildGoalRecord(
      final PostgresGoalRecord pgGoal,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp,
      final SchedulingGoalDSLCompilationService schedulingGoalDSLCompilationService) {
    final SchedulingDSL.GoalSpecifier goalSpecifier;
    try {
       goalSpecifier = schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
          pgGoal.definition(),
          "goals don't have names?");
    } catch (SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException | IOException e) {
      e.printStackTrace();
      throw new Error("");
    }
    final var goalId = new GoalId(pgGoal.id());

    final var goal = goalOfGoalSpecifier(goalSpecifier, horizonStartTimestamp, horizonEndTimestamp);

    return new GoalRecord(goalId, goal);
  }

  private static Goal goalOfGoalSpecifier(
      final SchedulingDSL.GoalSpecifier goalSpecifier,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp) {
    if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalDefinition g) {
      return goalOfGoalDefinition(g, horizonStartTimestamp, horizonEndTimestamp);
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalAnd g) {
      var builder = new CompositeAndGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.and(goalOfGoalSpecifier(subGoalSpecifier,
                                                  horizonStartTimestamp,
                                                  horizonEndTimestamp
        ));
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalOr g) {
      var builder = new OptionGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.or(goalOfGoalSpecifier(subGoalSpecifier,
                                                 horizonStartTimestamp,
                                                 horizonEndTimestamp
        ));
      }
      return builder.build();
    } else {
      throw new Error("Unhandled variant of GoalSpecifier:" + goalSpecifier);
    }
  }

  private static Goal goalOfGoalDefinition(
      final SchedulingDSL.GoalSpecifier.GoalDefinition goalDefinition,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp) {
    final var hor = new PlanningHorizon(
        Time.fromString(horizonStartTimestamp.toString()),
        Time.fromString(horizonEndTimestamp.toString())).getHor();
    return switch(goalDefinition.kind()) {
      case ActivityRecurrenceGoal -> new RecurrenceGoal.Builder()
          .forAllTimeIn(hor)
          .repeatingEvery(goalDefinition.interval())
          .thereExistsOne(makeActivityTemplate(goalDefinition))
          .build();
    };
  }

  private static ActivityCreationTemplate makeActivityTemplate(final SchedulingDSL.GoalSpecifier.GoalDefinition goalDefinition) {
    final var activityTemplate = goalDefinition.activityTemplate();
    var builder = new ActivityCreationTemplate.Builder()
        .ofType(new ActivityType(activityTemplate.activityType()));
    for (final var argument : activityTemplate.arguments().entrySet()) {
      builder = builder.withArgument(argument.getKey(), argument.getValue());
    }
    builder = builder.duration(Duration.ZERO);
    return builder.build();
  }
}
