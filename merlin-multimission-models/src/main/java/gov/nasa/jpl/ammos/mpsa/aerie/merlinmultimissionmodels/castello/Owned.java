package gov.nasa.jpl.ammos.mpsa.aerie.simulation.prototype;

/**
 * A marker class signifying a transfer of ownership of an object.
 */
public final class Owned<T> {
  public final T ref;

  private Owned(final T ref) {
    this.ref = ref;
  }

  public static <T> Owned<T> of(final T ref) {
    return new Owned<>(ref);
  }
}