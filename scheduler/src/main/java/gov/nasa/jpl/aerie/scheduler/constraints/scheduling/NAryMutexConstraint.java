package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityInstanceConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class implementing a n-ary mutex constraint between activity types
 */
public class NAryMutexConstraint extends GlobalConstraintWithIntrospection {

  Set<ActivityExpression> activityExpressions;

  public NAryMutexConstraint(ActivityExpression... activityExpressions){
    this.activityExpressions = new HashSet<>(Arrays.stream(activityExpressions).toList());
  }

  public Windows findWindows(Plan plan, Windows windows, Conflict conflict, final SimulationResults simulationResults) {
    if (conflict instanceof MissingActivityInstanceConflict) {
      return findWindows(plan, windows, ((MissingActivityInstanceConflict) conflict).getInstance().getType(), simulationResults);
    } else if (conflict instanceof MissingActivityTemplateConflict) {
      return findWindows(plan, windows, ((MissingActivityTemplateConflict) conflict).getGoal().getActTemplate().getType(), simulationResults);
    } else {
      throw new IllegalArgumentException("method implemented for two types of conflict");
    }
  }


  private Windows findWindows(Plan plan, Windows windows, ActivityType actToBeScheduled, final SimulationResults simulationResults) {
    Windows validWindows = new Windows(windows);
    for (var expression : activityExpressions) {
      if (!expression.getType().equals(actToBeScheduled)) {
        final var acts = new LinkedList<>(plan.find(expression, simulationResults));

        List<Window> rangesActs = acts
            .stream()
            .map(a -> Window.between(a.getStartTime(), a.getEndTime()))
            .collect(Collectors.toList());
        Windows twActs = new Windows(rangesActs);

        validWindows.subtractAll(twActs);
      }
    }
    return validWindows;
  }


  @Override
  public ConstraintState isEnforced(
      final Plan plan,
      final Windows windows,
      final SimulationResults simulationResults)
  {
    throw new IllegalArgumentException("Not implemented");
  }

}
