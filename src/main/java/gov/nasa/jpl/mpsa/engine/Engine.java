package gov.nasa.jpl.mpsa.engine;


import gov.nasa.jpl.mpsa.constraints.conditional.ConditionalConstraint;

public abstract class Engine {

    public Engine() {}


    public boolean evaluateConstraint(ConditionalConstraint expression){return false;}
}