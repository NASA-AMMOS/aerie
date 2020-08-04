package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

public class InvalidEntityException extends Exception {
  public InvalidEntityException() {
    super();
  }

  public InvalidEntityException(Exception source) {
    super(source);
  }
}

