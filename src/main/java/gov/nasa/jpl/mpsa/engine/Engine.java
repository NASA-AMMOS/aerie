package gov.nasa.jpl.mpsa.engine;


import gov.nasa.jpl.mpsa.constraints.conditional.ConditionalConstraint;

public abstract class Engine {

    public Engine() {}

    protected ConditionalConstraint expression;
    protected Boolean result;


    public abstract void evaluateNode();
    public abstract Boolean getResult();
}