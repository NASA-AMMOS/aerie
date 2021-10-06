package gov.nasa.jpl.aerie.scheduler;

import java.util.List;
import java.util.stream.Collectors;

public class BinaryMutexConstraint extends GlobalConstraint {

    ActivityType actType;
    ActivityType otherActType;

    public static BinaryMutexConstraint buildMutexConstraint(ActivityType type1, ActivityType type2){
        BinaryMutexConstraint mc = new BinaryMutexConstraint();
        mc.fill(type1, type2);
        return mc;
    }

    protected void fill(ActivityType type1, ActivityType type2){
        this.actType = type1;
        this.otherActType = type2;
    }



    public TimeWindows findWindows(Plan plan, TimeWindows windows, Conflict conflict) {
        if(conflict instanceof MissingActivityInstanceConflict){
            return findWindows(plan, windows, ((MissingActivityInstanceConflict) conflict).getInstance().getType());
        }
        else if(conflict instanceof MissingActivityTemplateConflict){
            return findWindows(plan, windows, ((MissingActivityTemplateConflict) conflict).getGoal().desiredActTemplate.type );
        }
        else{
            throw new IllegalArgumentException("method implemented for two types of conflict");
        }
    }



    private TimeWindows findWindows(Plan plan, TimeWindows windows, ActivityType actToBeScheduled) {

        if(!(actToBeScheduled.equals(actType) || actToBeScheduled.equals(otherActType))){
            throw new IllegalArgumentException("Activity type must be one of the mutexed types");
        }
        TimeWindows validWindows = new TimeWindows(windows);
        ActivityType actToBeSearched = actToBeScheduled.equals(actType) ? otherActType : actType;
        final var actSearch = new ActivityExpression.Builder()
                .ofType( actToBeSearched ).build();

        final var acts = new java.util.LinkedList<>( plan.find( actSearch ) );

        List<Range<Time>> rangesActs = acts.stream().map(a ->new Range<Time>(a.getStartTime(), a.getEndTime())).collect(Collectors.toList());
        TimeWindows twActs = TimeWindows.of(rangesActs);

        validWindows.substraction(twActs);

        return validWindows;
    }




    //Non-incremental checking
    //TODO: does not help finding where to put acts
    public ConstraintState isEnforced(Plan plan, TimeWindows windows){

        TimeWindows violationWindows = new TimeWindows();

        for(var window : windows.getRangeSet()){
            final var actSearch = new ActivityExpression.Builder()
                    .ofType( actType ).startsOrEndsIn(window).build();
            final var otherActSearch = new ActivityExpression.Builder()
                    .ofType( otherActType ).startsOrEndsIn(window).build();
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
        return cState;
    }

}
