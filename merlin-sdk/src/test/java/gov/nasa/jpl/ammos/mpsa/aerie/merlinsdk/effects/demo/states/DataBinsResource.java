package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DataBinsResource {
  private final Supplier<DataModel> activeModel;
  private final Consumer<Event> emitter;

  public DataBinsResource(final Supplier<DataModel> activeModel, final Consumer<Event> emitter) {
    this.activeModel = activeModel;
    this.emitter = emitter;
  }

  public DataBinResource bin(final String binName) {
    return new DataBinResource(() -> activeModel.get().getDataBin(binName), emitter, binName);
  }
}
