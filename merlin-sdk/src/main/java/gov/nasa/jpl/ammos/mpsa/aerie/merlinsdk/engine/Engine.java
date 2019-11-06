package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;


public abstract class Engine {

    public Engine() {}

    protected Boolean result;


    public abstract void evaluateNode();
    public abstract Boolean getResult();
}