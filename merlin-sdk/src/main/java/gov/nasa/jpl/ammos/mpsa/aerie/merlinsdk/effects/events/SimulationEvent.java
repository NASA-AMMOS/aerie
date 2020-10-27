package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.events;

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

}
