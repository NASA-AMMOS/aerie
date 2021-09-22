package gov.nasa.jpl.aerie.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class implementing a n-ary mutex constraint between activity types
 */
public class NAryMutexConstraint extends GlobalConstraint {

    Set<ActivityExpression> actTypes;

    public static NAryMutexConstraint buildMutexConstraint(List<ActivityExpression> actTypes){
        NAryMutexConstraint mc = new NAryMutexConstraint();
        mc.actTypes = new HashSet<ActivityExpression>(actTypes);
        return mc;
    }




    public TimeWindows findWindows( Plan plan, TimeWindows windows, Conflict conflict) {
        if(conflict instanceof MissingActivityInstanceConflict){
            return findWindows(plan, windows, ((MissingActivityInstanceConflict) conflict).getInstance().getType());
        }
        else if(conflict instanceof  MissingActivityTemplateConflict){
            return findWindows(plan, windows, ((MissingActivityTemplateConflict) conflict).getGoal().desiredActTemplate.type );
        }
        else{
            throw new IllegalArgumentException("method implemented for two types of conflict");
        }
    }



    private TimeWindows findWindows( Plan plan, TimeWindows windows, ActivityType actToBeScheduled) {
        TimeWindows validWindows = new TimeWindows(windows);
        for(var type : actTypes) {
            if(!type.equals(actToBeScheduled)) {
                //final var actSearch = ;

                final var acts = new java.util.LinkedList<>(plan.find(type));

                List<Range<Time>> rangesActs = acts.stream().map(a -> new Range<Time>(a.getStartTime(), a.getEndTime())).collect(Collectors.toList());
                TimeWindows twActs = TimeWindows.of(rangesActs);

                validWindows.substraction(twActs);
            }
        }
        return validWindows;
    }




    //Non-incremental checking
    //TODO: does not help finding where to put acts
    public ConstraintState isEnforced(Plan plan, TimeWindows windows){

        //TODO

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
