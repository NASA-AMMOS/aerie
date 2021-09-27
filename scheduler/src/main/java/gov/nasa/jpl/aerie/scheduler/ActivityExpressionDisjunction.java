package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class ActivityExpressionDisjunction extends ActivityExpression{


    List<ActivityExpression> actExpressions;

    protected ActivityExpressionDisjunction(List<ActivityExpression> actExpressions) {
        this.actExpressions = new ArrayList<ActivityExpression>(actExpressions);
    }

    /**
     *
     * @param act IN the activity to evaluate against the template criteria.
     *        not null.
     * @return true if the act instance matches one of the activity expression of the disjunction
     */
    @Override
    public boolean matches( @NotNull ActivityInstance act ) {
        for(var expr : actExpressions){
            if(expr.matches(act)){
                return true;
            }
        }

        return false;
    }


    }
