package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events;

public interface EventHandler<Result> {
    Result add(String stateName, double amount);
    Result set(String stateName, double value);
}
