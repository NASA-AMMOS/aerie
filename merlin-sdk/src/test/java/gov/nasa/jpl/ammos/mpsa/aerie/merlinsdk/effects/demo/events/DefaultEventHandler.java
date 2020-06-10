package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

// This can be mechanically derived from `EventHandler`.
public interface DefaultEventHandler<Result> extends EventHandler<Result> {
  Result unhandled();

  @Override
  default Result addDataRate(final String binName, final double amount) {
    return this.unhandled();
  }

  @Override
  default Result clearDataRate(final String binName) {
    return this.unhandled();
  }

  @Override
  default Result log(final String message) {
    return this.unhandled();
  }

  @Override
  default Result instantiateActivity(final String activityId, final String activityType) {
    return this.unhandled();
  }

  @Override
  default Result resumeActivity(final String activityId) {
    return this.unhandled();
  }
}
