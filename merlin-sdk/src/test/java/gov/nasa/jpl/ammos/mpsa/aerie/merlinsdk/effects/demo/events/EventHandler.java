package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

public interface EventHandler<Result> {
  Result addDataRate(String binName, double amount);
  Result clearDataRate(String binName);
  Result log(String message);
  Result run(String activityType);
}
