package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

public interface EventHandler<Result> {
  Result dataAdded(String binName, double amount);
  Result binCleared(String binName);
  Result log(String message);
  Result run(String activityType);
}
