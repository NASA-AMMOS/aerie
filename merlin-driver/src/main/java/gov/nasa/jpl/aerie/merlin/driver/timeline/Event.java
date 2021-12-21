package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** A heterogeneous event represented by a value and a topic over that value's type. */
public final class Event {
  private final Event.GenericEvent<?> inner;

  private Event(final Event.GenericEvent<?> inner) {
    this.inner = inner;
  }

  public static <EventType> Event create(
      final Topic<EventType> topic,
      final EventType event,
      final TaskId activeTask) {
    return new Event(new Event.GenericEvent<>(topic, event, activeTask));
  }

  public <EventType, Target>
  Optional<Target> extract(final Topic<EventType> topic, final Function<EventType, Target> transform) {
    return this.inner.extract(topic, transform);
  }

  public <EventType>
  Optional<EventType> extract(final Topic<EventType> topic) {
    return this.inner.extract(topic, $ -> $);
  }

  public Topic<?> topic() {
    return this.inner.topic();
  }

  public TaskId activeTask() {
    return this.inner.activeTask();
  }

  @Override
  public String toString() {
    return "<@%s, %s>".formatted(System.identityHashCode(this.inner.topic), this.inner.event);
  }

  private record GenericEvent<EventType>(Topic<EventType> topic, EventType event, TaskId activeTask) {
    private GenericEvent {
      Objects.requireNonNull(topic);
      Objects.requireNonNull(event);
    }

    private <Other, Target>
    Optional<Target> extract(final Topic<Other> otherTopic, final Function<Other, Target> transform) {
      if (this.topic != otherTopic) return Optional.empty();

      // SAFETY: If `this.topic` and `otherTopic` are identical references, then their types are also equal.
      //  So `Topic<EventType> = Topic<Other>`, and since Java generics are injective families, `EventType = Other`.
      @SuppressWarnings("unchecked")
      final var event = (Other) this.event;

      return Optional.of(transform.apply(event));
    }
  }
}
