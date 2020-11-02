package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.events;

import java.util.Optional;

public abstract class SimulationEvent<Event> {
  private SimulationEvent() {}

  public abstract <Result> Result visit(SimulationEventVisitor<Event, Result> visitor);

  public interface SimulationEventVisitor<Event, Result> {
    Result onInternalEvent(InternalEvent event);
    Result onAdaptationEvent(Event event);
  }

  public static <Event> SimulationEvent<Event> ofInternalEvent(final InternalEvent event) {
    return new SimulationEvent<>() {
      @Override
      public <Result> Result visit(final SimulationEventVisitor<Event, Result> visitor) {
        return visitor.onInternalEvent(event);
      }
    };
  }

  public static <Event> SimulationEvent<Event> ofAdaptationEvent(final Event event) {
    return new SimulationEvent<>() {
      @Override
      public <Result> Result visit(final SimulationEventVisitor<Event, Result> visitor) {
        return visitor.onAdaptationEvent(event);
      }
    };
  }

  public static <Event> Optional<Event> asAdaptationEvent(final SimulationEvent<Event> event) {
    return event.visit(
        new SimulationEvent.SimulationEventVisitor<>() {
          @Override
          public Optional<Event> onInternalEvent(final InternalEvent event) {
            return Optional.empty();
          }

          @Override
          public Optional<Event> onAdaptationEvent(final Event event) {
            return Optional.of(event);
          }
        }
    );
  }

  public static <Event> Optional<InternalEvent> asInternalEvent(final SimulationEvent<Event> event) {
    return event.visit(
        new SimulationEvent.SimulationEventVisitor<>() {
          @Override
          public Optional<InternalEvent> onInternalEvent(final InternalEvent event) {
            return Optional.of(event);
          }

          @Override
          public Optional<InternalEvent> onAdaptationEvent(final Event event) {
            return Optional.empty();
          }
        }
    );
  }

}
