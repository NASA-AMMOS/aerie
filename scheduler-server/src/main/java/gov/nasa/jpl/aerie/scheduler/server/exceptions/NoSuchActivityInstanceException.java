package gov.nasa.jpl.aerie.scheduler.server.exceptions;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;

public class NoSuchActivityInstanceException extends Exception {
  private final ActivityDirectiveId id;

  public NoSuchActivityInstanceException(final ActivityDirectiveId id) {
    super("No activity instance exists with id `" + id + "`");
    this.id = id;
  }

  public ActivityDirectiveId getInvalidPlanId() {
    return this.id;
  }
}
