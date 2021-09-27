package gov.nasa.jpl.aerie.scheduler;

import java.util.function.Function;

public class FilterUserFunctional extends FilterFunctional {

    Function<Range<Time>, Boolean> function;

    public FilterUserFunctional(Function<Range<Time>, Boolean> function){
        this.function = function;
    }

    @Override
    public boolean shouldKeep(Plan plan, Range<Time> range) {
        return function.apply(range);
    }
}
