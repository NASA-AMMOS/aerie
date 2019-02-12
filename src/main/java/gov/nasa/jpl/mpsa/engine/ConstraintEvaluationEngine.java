package gov.nasa.jpl.mpsa.engine;

import gov.nasa.jpl.mpsa.constraints.conditional.ConditionalConstraint;
import gov.nasa.jpl.mpsa.resources.Resource;

public class ConstraintEvaluationEngine<V, T extends Comparable> extends Engine{

    private ConditionalConstraint expression;
    private Boolean result;


    public ConstraintEvaluationEngine(ConditionalConstraint expression){
        this.expression = expression;
    }



    public Boolean getResult(){
        return this.result;
    }

    public boolean isLeaf(){
        return ((expression.getLeftLeaf() != null) && (expression.getRightLeaf() != null));
    }


    //after evaluation, property change listeners will notify parents if value changes
    public void evaluateConstraintNodes(){

        assert(!isLeaf());
        assert(expression.getLeft()!=null && expression.getRight()!=null);

        if (expression.getOperation().equals("||")){
            this.result = (expression.getLeft().getValue() || expression.getRight().getValue());
        }

        else if (expression.getOperation().equals("&&")){
            this.result = (expression.getLeft().getValue() && expression.getRight().getValue());
        }

        else {
            throw new IllegalArgumentException("Operation not recognized");
        }
    }

    public void evaluateNode() {
        if (isLeaf()) {
            evaluateLeafNodes();
        } else {
            evaluateConstraintNodes();
        }
    }


    public void evaluateLeafNodes(){

        assert(isLeaf());
        T leftVal = (T)expression.getLeftLeaf().getCurrentValue();
        V rightLeaf = (V)expression.getRightLeaf();
        String operation = expression.getOperation();
        switch (operation)
        {
            case ">":
                if ((Double) leftVal > (Double) rightLeaf){ this.result = true;}
                else {this.result = false;}
                break;
            case ">=":
                if ((Double) leftVal >= (Double) rightLeaf){ this.result = true;}
                else {this.result = false;}
                break;
            case "==":
                if ((Double) leftVal == (Double) rightLeaf){ this.result = true;}
                else {this.result = false;}
                break;
            case "<=":
                if ((Double) leftVal <= (Double) rightLeaf){ this.result = true;}
                else {this.result = false;}
                break;
            case "<":
                if ((Double) leftVal < (Double) rightLeaf){ this.result = true;}
                else {this.result = false;}
                break;
            case "!=":
                if ((Double) leftVal != (Double) rightLeaf){ this.result = true;}
                else {this.result = false;}
                break;
            default:
                throw new IllegalArgumentException("Operation not recognized");

        }
        return;
    }



    public boolean evaluateConstraint(ConditionalConstraint expression){
        return false;
    }


}