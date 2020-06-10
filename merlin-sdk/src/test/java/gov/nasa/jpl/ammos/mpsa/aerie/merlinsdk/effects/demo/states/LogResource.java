package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;

import java.util.function.Consumer;

public final class LogResource implements CumulableResource<String> {
  private final Consumer<Event> emitter;

  public LogResource(final Consumer<Event> emitter) {
    this.emitter = emitter;
  }

  @Override
  public void add(final String message) {
    emitter.accept(Event.log(message));
  }
}
