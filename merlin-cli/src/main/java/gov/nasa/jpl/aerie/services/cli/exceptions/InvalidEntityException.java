package gov.nasa.jpl.aerie.services.cli.exceptions;

public class InvalidEntityException extends Exception {
  public InvalidEntityException() {}
  public InvalidEntityException(Exception e) {
    super(e);
  }
}
