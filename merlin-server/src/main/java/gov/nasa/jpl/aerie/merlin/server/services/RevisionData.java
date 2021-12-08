package gov.nasa.jpl.aerie.merlin.server.services;

public interface RevisionData {
  record ValidationResult(boolean result, String reason) {}
  ValidationResult validate(final RevisionData other);
}
