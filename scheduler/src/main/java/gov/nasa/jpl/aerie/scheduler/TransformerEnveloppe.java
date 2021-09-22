package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

public class TransformerEnveloppe implements TimeWindowsTransformer{

    List<TimeRangeExpression> insideExprs;


    public TransformerEnveloppe(List<TimeRangeExpression> insideExprs){
        this.insideExprs = insideExprs;
    }

    @Override
    public TimeWindows transformWindows(Plan plan, TimeWindows windows) {

        TimeWindows ret = new TimeWindows();

        Time min = windows.getMaximum(), max = windows.getMinimum();
        boolean present = true;
        for(var insideExpr : insideExprs){

            var rangeExpr = insideExpr.computeRange(plan, windows);
            if(rangeExpr.isEmpty()){
                present = false;
                break;
            } else{
                min = Time.min(min, rangeExpr.getMinimum());
                max = Time.max(max, rangeExpr.getMaximum());
            }
        }

        if(present){
            //register new transformed window
            ret.union(new Range<Time>(min, max));
        }


        return ret;

    }
}
