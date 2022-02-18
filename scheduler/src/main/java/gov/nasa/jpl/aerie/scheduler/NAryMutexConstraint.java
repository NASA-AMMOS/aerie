package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

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

  public static NAryMutexConstraint buildMutexConstraint(List<ActivityExpression> actTypes) {
    NAryMutexConstraint mc = new NAryMutexConstraint();
    mc.activityExpressions = new HashSet<>(actTypes);
    return mc;
  }


  public Windows findWindows(Plan plan, Windows windows, Conflict conflict) {
    if (conflict instanceof MissingActivityInstanceConflict) {
      return findWindows(plan, windows, ((MissingActivityInstanceConflict) conflict).getInstance().getType());
    } else if (conflict instanceof MissingActivityTemplateConflict) {
      return findWindows(plan, windows, ((MissingActivityTemplateConflict) conflict).getGoal().desiredActTemplate.type);
    } else {
      throw new IllegalArgumentException("method implemented for two types of conflict");
    }
  }


  private Windows findWindows(Plan plan, Windows windows, ActivityType actToBeScheduled) {
    Windows validWindows = new Windows(windows);
    for (var expression : activityExpressions) {
      if (!expression.type.equals(actToBeScheduled)) {
        final var acts = new LinkedList<>(plan.find(expression));

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


  //Non-incremental checking
  //TODO : uncomment and verify
  public ConstraintState isEnforced(Plan plan, Windows windows) {

        /*TimeWindows violationWindows = new TimeWindows();

        for(var window : windows.getRangeSet()){
            final var actSearch = new ActivityExpression.Builder()
                    .type( actType ).startsOrEndsIn(window).build();
            final var otherActSearch = new ActivityExpression.Builder()
                    .type( otherActType ).startsOrEndsIn(window).build();
            final var acts = new java.util.LinkedList<>( plan.find( actSearch ) );
            final var otherActs = new java.util.LinkedList<>( plan.find( otherActSearch ) );

            List<Range<Time>> rangesActs = acts.stream().map(a ->new Range<Time>(a.getStartTime(), a.getEndTime())).collect(Collectors.toList());
            TimeWindows twActs = TimeWindows.of(rangesActs);
            List<Range<Time>> rangesOtherActs = otherActs.stream().map(a ->new Range<Time>(a.getStartTime(), a.getEndTime())).collect(Collectors.toList());
            TimeWindows twOtherActs =TimeWindows.of(rangesOtherActs);

            TimeWindows result = new TimeWindows(twActs);
            result.intersection(twOtherActs);
            //intersection with current window to be sure we are not analyzing intersections happenning outside
            result.intersection(window);
            violationWindows.union(result);
        }
        ConstraintState cState;
        if(!violationWindows.isEmpty()){
            cState = new ConstraintState(this,true, violationWindows);
        } else{
            cState = new ConstraintState(this,false, null);
        }
        return cState;*/
    return null;
  }

}
