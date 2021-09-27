package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class ActivityCreationTemplateDisjunction extends ActivityCreationTemplate {


    List<ActivityCreationTemplate> acts;

    protected ActivityCreationTemplateDisjunction(List<ActivityCreationTemplate> acts){
        assert(acts.size()>0);
        this.acts = new ArrayList<ActivityCreationTemplate>(acts);
    }

    /**
     * generate a new activity instance based on template defaults
     *
     * @param name IN the activity instance identifier to associate to the
     *        newly constructed activity instance
     * @return a newly constructed activity instance with values chosen
     *         according to any specified template criteria, the first of the disjunction
     */
    @Override
    public @NotNull ActivityInstance createActivity( String name ) {
        //TODO: returns first ACT of disjunction, change it
       return acts.get(0).createActivity(name);

    }

    /**
     *
     * @param act IN the activity to evaluate against the template criteria.
     *        not null.
     * @return true if the act instance matches one of the activity expression of the disjunction
     */
    @Override
    public boolean matches( @NotNull ActivityInstance act ) {
        for(var expr :acts ){
            if(expr.matches(act)){
                return true;
            }
        }

        return false;
    }

}
