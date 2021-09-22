package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

/**
 * A latching time expression is computed with one expression if on the first element of a larger sequence and with another time expression if on the
 * remaining element of the larger sequence
 */
public class TimeExpressionLatching extends TimeExpression {
    public TimeExpressionLatching(TimeExpression expr1, TimeExpression expr2, TimeRangeExpression expr) {
        this.expr1 = expr1;
        this.expr2 = expr2;
        resetWindowsExpression =expr;
    }


    TimeExpression expr1;
    TimeExpression expr2;
    TimeRangeExpression resetWindowsExpression;


    @Override
    public Range<Time> computeTime(Plan plan, Range<Time> interval) {

        List<Range<Time>> resetPeriods = resetWindowsExpression.computeRange(plan, TimeWindows.spanMax()).getRangeSet();

        boolean first = true;
        for(var window : resetPeriods) {
            Range<Time> inter = window.intersect(interval);
            if (inter != null) {
                if (first) {
                    return expr1.computeTime(plan, interval);
                } else {
                    return expr2.computeTime(plan, interval);
                }

            }
            first = false;
        }
        return null;
    }
}
