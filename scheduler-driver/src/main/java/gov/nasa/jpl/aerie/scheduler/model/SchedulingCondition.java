package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityInstanceConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.ConstraintState;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.GlobalConstraintWithIntrospection;

import java.util.List;

public record SchedulingCondition(
    Expression<Windows> expression,
    List<ActivityType> activityTypes
) implements GlobalConstraintWithIntrospection
{
  @Override
  public Windows findWindows(
      final Plan plan,
      final Windows windows,
      final Conflict conflict,
      final SimulationResults simulationResults,
      final EvaluationEnvironment evaluationEnvironment)
  {
    final ActivityType type;
    if (conflict instanceof MissingActivityInstanceConflict c) {
      type = c.getInstance().getType();
    } else if (conflict instanceof MissingActivityTemplateConflict c) {
      type = c.getActTemplate().type();
    } else {
      throw new Error("Unsupported conflict %s".formatted(conflict));
    }
    if(anyMatch(this.activityTypes, type)){
      return this.expression.evaluate(simulationResults, evaluationEnvironment).and(windows);
    } else {
      return windows;
    }
  }

  @Override
  public ConstraintState isEnforced(
      final Plan plan,
      final Windows windows,
      final SimulationResults simulationResults)
  {
    // A SchedulingCondition is never "violated" per se - if there are no windows in which
    // activities can be placed, that does not mean that it has been violated.
    // TODO: As of writing isEnforced is unused. Either remove or come up with a more coherent plan for GlobalConstraints.
    return new ConstraintState(this, false, null);
  }

  static boolean anyMatch(final List<ActivityType> activityTypes, final ActivityType type) {
    // TODO we may want to handle more complex activity expressions, not just type.
    for (final var activityType : activityTypes) {
      if (type.equals(activityType)) {
        return true;
      }
    }
    return false;
  }
}
