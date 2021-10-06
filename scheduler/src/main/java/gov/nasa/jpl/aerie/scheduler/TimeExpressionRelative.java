package gov.nasa.jpl.aerie.scheduler;

import java.util.Map;

public class TimeExpressionRelative extends TimeExpression{

    protected TimeAnchor anchor;
    protected boolean fixed = true;
    protected String name;

    public TimeExpressionRelative(TimeAnchor anchor, boolean fixed, String name){
        this.fixed = fixed;
        this.anchor = anchor;
        this.name = name;
    }

    @Override
    public Range<Time> computeTime(Plan plan, Range<Time> interval) {
        Time from = null;
        if(anchor == TimeAnchor.START) {
            from = interval.getMinimum();
        } else if(anchor == TimeAnchor.END){
            from= interval.getMaximum();
        }

        Time res = from;
        for(Map.Entry<Time.Operator, Duration> entry : this.operations.entrySet()) {
            res = Time.performOperation(entry.getKey(), res, entry.getValue());
        }

        Range<Time> retRange;

        //if we want an range of possibles
        if(!fixed){
            if(res.compareTo(from) > 0){
                retRange = new Range<Time>(from, res);

            } else{
                retRange = new Range<Time>(res, from);
            }
    // we just want to compute the absolute timepoint
        } else{
            retRange = new Range<Time>(res, res);
        }

        return retRange;
    }
}
