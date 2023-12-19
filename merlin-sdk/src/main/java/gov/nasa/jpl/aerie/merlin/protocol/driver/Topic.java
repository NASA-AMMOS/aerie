package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.Capability;

/**
 * An unforgeable token identifying a particular stream of events.
 *
 * Every {@code Topic} instance identifies a unique topic, even if two topics share the same {@code EventType}.
 */
@Capability
public final class Topic<EventType> {
  private final String displayName;

  public Topic(String displayName) {
    this.displayName = displayName;
  }

  public Topic() {
    this.displayName = null;
  }

  @Override
  public String toString() {
    if (displayName == null) return "@" + System.identityHashCode(this);
    return displayName;
  }
}
