package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/** A time range expression combines timewindows from states, activity expressions, state expressions, and other time
 * range expressions and allows to apply a sequence of filters and transforms to be used in goals
 *
 */
public class TimeRangeExpression {

    /**
     * TODO: for now, acts like a big AND. We need a OR.
     * @param plan x
     * @param windows x
     * @return x
     */
    public TimeWindows computeRange(Plan plan, TimeWindows windows) {

        TimeWindows inter = new TimeWindows(windows);
        //particularly important for combining with constant states as they are by definition adjacent values
        inter.doNotMergeAdjacent();

        if(actTemplate != null) {
            final var anchorActSearch = new ActivityExpression.Builder()
                    .basedOn(actTemplate)
                    .startsIn(new Range<Time>(windows.getMinimum(), windows.getMaximum())).build();
            final var anchorActs = plan.find(anchorActSearch);
            for (var anchorAct : anchorActs) {
                inter.union(new Range<Time>(anchorAct.getStartTime(), anchorAct.getEndTime()));
            }
        }

        for(var otherExpr : timeRangeExpressions){
            TimeWindows windowsState = otherExpr.computeRange(plan, windows);
            inter.intersection(windowsState);
            if (inter.isEmpty()) {
                break;
            }
        }

        for (var expr : stateExpr) {
            TimeWindows windowsState =  expr.findWindows(plan, windows);
            inter.intersection(windowsState);
            if (inter.isEmpty()) {
                break;
            }
        }

        for (var constState : constantsStates) {
            TimeWindows windowsState = TimeWindows.of(constState.getTimeline(windows).keySet(), true);
            inter.intersection(windowsState);
            if (inter.isEmpty()) {
                break;
            }
        }
        for (var filterOrtransform : filtersAndTransformers) {
            if(filterOrtransform instanceof TimeWindowsFilter){
                inter = ((TimeWindowsFilter)filterOrtransform).filter(plan, inter);
            } else if(filterOrtransform instanceof  TimeWindowsTransformer){
                inter = ((TimeWindowsTransformer)filterOrtransform).transformWindows(plan, inter);
            }
            if(inter.isEmpty()){
                break;
            }
        }
        return inter;

    }


    protected String name = "TRE_" + new Random().nextInt();
    protected List<TimeRangeExpression> timeRangeExpressions;
    protected List<Object> filtersAndTransformers;
    protected List<StateConstraintExpression> stateExpr;
    protected List<ExternalState<?>> constantsStates;
    private ActivityExpression actTemplate;

    //TODO:unused now, not sure it is useful
    protected Range<Time> horizon;


    public static TimeRangeExpression constantValuesOf(ExternalState<?> sce){
        TimeRangeExpression tre = new TimeRangeExpression.Builder().ofEachValue(sce).build();
        return tre;
    }

    public static class Builder  {
        List<Object> filtersAndTransformers = new ArrayList<Object>();
        List<StateConstraintExpression> stateExpr = new ArrayList<StateConstraintExpression>();
        List<ExternalState<?>> constantsStates = new ArrayList<ExternalState<?>>();
        List<TimeRangeExpression> timeRangeExpressions = new ArrayList<TimeRangeExpression>();;

        Range<Time> horizon = null;
        private ActivityExpression actTemplate;


        public TimeRangeExpression.Builder thenFilter(TimeWindowsFilter filter){
            filtersAndTransformers.add(filter);
            return getThis();
        }

        public TimeRangeExpression.Builder thenFilter(Function<Range<Time>, Boolean> functionalFilter){
            filtersAndTransformers.add(Filters.functionalFilter(functionalFilter));
            return getThis();
        }

        public TimeRangeExpression.Builder thenTransform(TimeWindowsTransformer transformer){
            filtersAndTransformers.add(transformer);
            return getThis();
        }


        public TimeRangeExpression.Builder onHorizon(Range<Time> horizon){
            this.horizon = horizon;
            return getThis();
        }

        /**
         * state constraint use case
         * @param expr x
         * @return x
         */
        public TimeRangeExpression.Builder from(StateConstraintExpression expr){
            this.stateExpr.add(expr);
            return getThis();
        }

        public TimeRangeExpression.Builder from(TimeRangeExpression expr){
            this.timeRangeExpressions.add(expr);
            return getThis();
        }

        public TimeRangeExpression.Builder from(ActivityExpression actTemplate){
            this.actTemplate = actTemplate;
            return getThis();
        }

        String name;
        public Builder name(String name){
            this.name = name;
            return this;
        }

        /**
         * relative instant use case
         * @param state x
         * @param <T> x
         * @return x
         */
        public <T> TimeRangeExpression.Builder ofEachValue(ExternalState<T> state){
            this.constantsStates.add(state);
            return getThis();

        }


        public TimeRangeExpression build(){
            TimeRangeExpression tre = new TimeRangeExpression();

            if(constantsStates.isEmpty() && stateExpr.isEmpty() && actTemplate == null && timeRangeExpressions.isEmpty()){
                throw new RuntimeException("either from or constantValuesOf has to be used to build a valid expression");
            }
            tre.filtersAndTransformers = filtersAndTransformers;
            tre.constantsStates = constantsStates;
            tre.stateExpr = stateExpr;
            tre.horizon = horizon;
            tre.timeRangeExpressions = timeRangeExpressions;
            tre.actTemplate = actTemplate;
            if(name != null) {
                tre.name = name;
            }
            return tre;
        }

        public TimeRangeExpression.Builder getThis(){
            return this;
        }

    }


    private TimeRangeExpression(){

    }

}
