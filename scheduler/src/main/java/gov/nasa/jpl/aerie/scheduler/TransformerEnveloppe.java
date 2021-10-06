package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

public class TransformerEnveloppe implements TimeWindowsTransformer{

    List<TimeRangeExpression> insideExprs;


    public TransformerEnveloppe(List<TimeRangeExpression> insideExprs){
        this.insideExprs = insideExprs;
    }

    @Override
    public TimeWindows transformWindows(Plan plan, TimeWindows windowsToTransform) {

        TimeWindows ret = new TimeWindows();

        Time min = windowsToTransform.getMaximum(), max = windowsToTransform.getMinimum();
        boolean atLeastOne = false;
        for(var insideExpr : insideExprs){

            var rangeExpr = insideExpr.computeRange(plan, windowsToTransform);
            if(!rangeExpr.isEmpty()){
                atLeastOne = true;
                min = Time.min(min, rangeExpr.getMinimum());
                max = Time.max(max, rangeExpr.getMaximum());
            }
        }

        if(atLeastOne){
            //register new transformed window
            ret.union(new Range<Time>(min, max));
        }


        return ret;

    }
}
