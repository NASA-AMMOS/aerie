package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class OptionGoal extends Goal {

    Range<Integer> namongp;

    List<Goal> goals;
    Optimizer optimizer;

    public List<Goal> getSubgoals(){
        return goals;
    }

    @Override
    public java.util.Collection<Conflict> getConflicts( Plan plan ) {
    return null;

    }

    public static class Builder {

        List<Goal> goals = new ArrayList<Goal>();
        private String name;

        enum CHOICE{
            ATLEAST,
            ATMOST,
            EXACTLY
        }
        Optimizer optimizer;

        int namongp=0;
        CHOICE choice = null;

        public Builder exactlyOneOf(){
            if(choice != null){
                throw new IllegalArgumentException("Choice of goal satisfaction has been done already");
            }
            choice = CHOICE.EXACTLY;
            namongp= 1;
            return this;
        }

        public Builder named(String name){
            this.name = name;
            return this;
        }

        public Builder atLeast(int n){

            if(choice != null){
                throw new IllegalArgumentException("Choice of goal satisfaction has been done already");
            }
            namongp= n;
            choice = CHOICE.ATLEAST;
            return this;
        }


        public Builder atMost(int n){

            if(choice != null){
                throw new IllegalArgumentException("Choice of goal satisfaction has been done already");
            }
            namongp= n;
            choice = CHOICE.ATMOST;
            return this;
        }

        public Builder or(Goal goal){
            goals.add(goal);
            return this;
        }

        public Builder optimizingFor(Optimizer s){
            optimizer = s;
            return this;
        }

        public OptionGoal build(){

            OptionGoal dg = new OptionGoal();
            dg.goals = goals;
            dg.name = name;
            dg.optimizer = optimizer;
            switch(choice){
                case ATLEAST:
                    dg.namongp = new Range<Integer>(this.namongp, goals.size());
                    break;
                case ATMOST:
                    dg.namongp = new Range<Integer>(0,this.namongp);
                    break;
                case EXACTLY:
                    dg.namongp = new Range<Integer>(this.namongp,this.namongp);
                    break;
            }

            return dg;

        }

    }

}
