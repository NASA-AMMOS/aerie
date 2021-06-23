package gov.nasa.jpl.aerie.merlin.server.services;

public class UnexpectedSubtypeError extends Error {
  public <E> UnexpectedSubtypeError(final Class<E> supertype, final E instance) {
    super("Unexpected subtype %s of type %s".formatted(
        instance.getClass().getCanonicalName(),
        supertype.getCanonicalName()));
  }
}
