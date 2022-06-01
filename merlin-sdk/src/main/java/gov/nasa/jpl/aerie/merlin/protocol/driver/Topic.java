package gov.nasa.jpl.aerie.merlin.protocol.driver;

/**
 * An unforgeable token identifying a particular family of events.
 *
 * Every {@code Topic} instance identifies a unique topic, even if two topics share the same {@code EventType}.
 */
public final class Topic<EventType> {
  @Override
  public String toString() {
    return "@" + System.identityHashCode(this);
  }
}
