package gov.nasa.jpl.aerie.scheduler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CardinalityConstraint extends GlobalConstraint {

    private int max;
    private Range<Time> interval;
    private ActivityType type;
    private ActivityInstance instance;

    /**
     * builder to create different flavors of cardinality constraints
     * - one checking if a specific instance of an activity is present in the plan
     * -  one checking if at most a desired number of activities of a specific activity type are present in the plan
     */
    public static class Builder{

        private int max = -1;
        private ActivityType type;
        private Range<Time> interval;
        private ActivityInstance instance;

        public  CardinalityConstraint.Builder type(ActivityType type){
            this.type = type;
            return getThis();
        }

        public  CardinalityConstraint.Builder instanceIsPresent(ActivityInstance instance){
            this.instance = instance;
            return getThis();
        }


        public  CardinalityConstraint.Builder inInterval(Range<Time> interval){
            this.interval = interval;
            return getThis();
        }


        public  CardinalityConstraint.Builder atMost(int max){
            this.max = max;
            return getThis();

        }



        public CardinalityConstraint build() { return fill( new CardinalityConstraint() ); }

        /**
         * {@inheritDoc}
         */
         protected CardinalityConstraint.Builder  getThis() { return this; }

        /**
         * populates the provided goal with specifiers from this builder and above
         *
         * typically called by any derived builder classes to fill in the
         * specifiers managed at this builder level and above
         *
         * @param constraint IN/OUT a goal object to be filled with specifiers from this
         *        level of builder and above
         * @return the provided object, with details filled in
         */
        protected CardinalityConstraint fill( CardinalityConstraint constraint ) {

            if(max == -1){throw new IllegalArgumentException(
                    "requires at least one non-null bound" ); }
            if(interval== null){
                throw new IllegalArgumentException(
                        "requires a validity interval" );
            }
            if(type ==null){
                throw new IllegalArgumentException(
                        "requires an activity type" );
            }

            if(instance != null ){
                if((max != -1 || type != null)) {
                    throw new IllegalArgumentException(
                            "if you use isInstancePresent() predicate, no min, max or type can be specified as it verifies the presence of a unique instance of an activity");
                }
                max = 1;
                type = instance.getType();
            }

            constraint.interval=interval;
            constraint.max=max;
            constraint.type=type;
            constraint.instance = instance;
            return constraint;
        }

    }//Builder

    private CardinalityConstraint(){

    }




    //Non-incremental checking
    public ConstraintState isEnforced(Plan plan, TimeWindows windows){

        int nbAct = 0;
        TimeWindows evalSet = new TimeWindows(windows);
        evalSet.intersection(this.interval);

        //to avoid recounting twice the same activities in case they span over multiples windows
        Set<ActivityInstance> allActs = new HashSet<ActivityInstance>();

        TimeWindows violationWindows = new TimeWindows();

        for(var window : evalSet.getRangeSet()){

            var actSearch = new ActivityExpression.Builder()
                    .ofType( this.type ).startsOrEndsIn(window).build();
            if(instance != null){
                actSearch = new ActivityExpression.Builder()
                        .basedOn( instance ).startsOrEndsIn(window).build();
            }

            final var acts = new java.util.LinkedList<>( plan.find( actSearch ) );
            allActs.addAll(acts);
        }
        boolean violated = false;
        String explanation="";
        if(max != -1 && allActs.size() > max){
            violated = true;
            explanation += "required cardinality <= " + max  + ", actual number of activities = " + allActs.size()+"\n";
        }
        ConstraintState cState = new ConstraintState(this,violated, evalSet);

        return cState;
    }


    public Collection<ActivityInstance> getAllActs(Plan plan, TimeWindows windows){

        int nbAct = 0;
        TimeWindows evalSet = new TimeWindows(windows);
        evalSet.intersection(this.interval);

        //to avoid recounting twice the same activities in case they span over multiples windows
        Set<ActivityInstance> allActs = new HashSet<ActivityInstance>();

        TimeWindows violationWindows = new TimeWindows();

        for(var window : evalSet.getRangeSet()){

            var actSearch = new ActivityExpression.Builder()
                    .ofType( this.type ).startsOrEndsIn(window).build();
            if(instance != null){
                actSearch = new ActivityExpression.Builder()
                        .basedOn( instance ).startsOrEndsIn(window).build();
            }

            final var acts = new java.util.LinkedList<>( plan.find( actSearch ) );
            allActs.addAll(acts);
        }

        return allActs;
    }

    @Override
    public TimeWindows findWindows(Plan plan, TimeWindows windows, Conflict conflict) {
        TimeWindows intersect = new TimeWindows(windows);
        intersect.intersection(this.interval);
        if(!intersect.isEmpty()) {
            final var allActs = getAllActs(plan, intersect);
            if(allActs.size() < max){
                return windows;
            } else{
                windows.substraction(intersect);
                return windows;
            }
        } else{
            return windows;
        }
    }


}
