package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelQuerier;

import java.util.function.Consumer;

public final class DataBinsResource {
  private final DataModelQuerier querier;
  private final Consumer<Event> emitter;

  public DataBinsResource(final DataModelQuerier querier, final Consumer<Event> emitter) {
    this.querier = querier;
    this.emitter = emitter;
  }

  public DataBinResource bin(final String binName) {
    return new DataBinResource(
        this.querier.getBin(binName),
        (delta) -> this.emitter.accept(Event.addDataRate(binName, delta)));
  }
}
