package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.conditional.ConditionalConstraint;

public abstract class Engine {

    public Engine() {}

    protected ConditionalConstraint expression;
    protected Boolean result;


    public abstract void evaluateNode();
    public abstract Boolean getResult();
}