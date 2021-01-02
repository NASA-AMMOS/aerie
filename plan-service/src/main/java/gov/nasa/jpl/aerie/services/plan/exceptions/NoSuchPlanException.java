package gov.nasa.jpl.aerie.services.plan.exceptions;

public class NoSuchPlanException extends Exception {
  private final String id;

  public NoSuchPlanException(final String id) {
    super("No plan exists with id `" + id + "`");
    this.id = id;
  }

  public String getInvalidPlanId() {
    return this.id;
  }
}
