package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events;

public interface IndependentStateEventHandler<Result> {
    Result add(String stateName, double amount);
    Result set(String stateName, double value);
}
