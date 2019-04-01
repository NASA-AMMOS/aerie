package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.conditional.ConditionalConstraint;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ConstraintEvaluationEngine<V, T extends Comparable> extends Engine{

    private ScriptEngineManager mgr;
    private ScriptEngine engine;


    public ConstraintEvaluationEngine(ConditionalConstraint expression){
        this.expression = expression;
        this.mgr = new ScriptEngineManager();
        this.engine = mgr.getEngineByName("JavaScript");
    }

    @Override
    public Boolean getResult(){
        return this.result;
    }

    @Override
    public void evaluateNode() {
        if (isLeaf()) {
            evaluateLeafNodes();
        } else {
            evaluateConstraintNodes();
        }
    }

    public boolean isLeaf(){
        return ((expression.getLeftLeaf() != null) && (expression.getRightLeaf() != null));
    }

    public void evaluateEngine(String equation){
        try {
            this.result = (Boolean) engine.eval(equation);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    //after evaluation, property change listeners will notify parents if value changes
    public void evaluateConstraintNodes(){
        assert(!isLeaf());
        assert(expression.getLeft()!=null && expression.getRight()!=null);
        String equation = expression.getLeft().getValue().toString() + expression.getOperation() +
                expression.getRight().getValue().toString();
        evaluateEngine(equation);
    }


    public void evaluateLeafNodes(){
        String equation = expression.getLeftLeaf().getCurrentValue().toString() + expression.getOperation() +
                expression.getRightLeaf().toString();
        evaluateEngine(equation);
    }

}