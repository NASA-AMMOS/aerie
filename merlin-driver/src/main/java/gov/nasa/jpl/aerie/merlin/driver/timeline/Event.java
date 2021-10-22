package gov.nasa.jpl.aerie.merlin.driver.timeline;

/** A heterogeneous event represented by a value and a topic over that value's type. */
public final class Event {
  private final Event.GenericEvent<?> inner;

  private Event(final Event.GenericEvent<?> inner) {
    this.inner = inner;
  }

  public static <EventType> Event create(final Topic<EventType> topic, final EventType event) {
    return new Event(new Event.GenericEvent<>(topic, event));
  }

  public <EventType> EventType extract(final Topic<EventType> topic) {
    return this.inner.extract(topic);
  }

  @Override
  public String toString() {
    return "<@%s, %s>".formatted(System.identityHashCode(this.inner.topic), this.inner.event);
  }

  private record GenericEvent<EventType>(Topic<EventType> topic, EventType event) {
    private <Other> Other extract(final Topic<Other> otherTopic) {
      if (this.topic != otherTopic) return null;

      // SAFETY: If `this.topic` and `otherTopic` are identical references, then their types are also equal.
      //  So `Topic<EventType> = Topic<Other>`, and since Java generics are injective families, `EventType = Other`.
      @SuppressWarnings("unchecked")
      final var event = (Other) this.event;

      return event;
    }
  }
}
