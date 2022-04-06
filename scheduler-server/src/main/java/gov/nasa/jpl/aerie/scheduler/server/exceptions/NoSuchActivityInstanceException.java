package gov.nasa.jpl.aerie.scheduler.server.exceptions;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;

public class NoSuchActivityInstanceException extends Exception {
  private final ActivityInstanceId id;

  public NoSuchActivityInstanceException(final ActivityInstanceId id) {
    super("No activity instance exists with id `" + id + "`");
    this.id = id;
  }

  public ActivityInstanceId getInvalidPlanId() {
    return this.id;
  }
}
