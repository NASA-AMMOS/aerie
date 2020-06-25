package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DataBinsResource {
  private final Function<String, Double> getVolumeOf;
  private final Function<String, Double> getRateOf;
  private final Consumer<Event> emitter;

  public DataBinsResource(final Function<String, Double> getVolumeOf, final Function<String, Double> getRateOf, final Consumer<Event> emitter) {
    this.getVolumeOf = getVolumeOf;
    this.getRateOf = getRateOf;
    this.emitter = emitter;
  }

  public DataBinResource bin(final String binName) {
    return new DataBinResource(binName, this.getVolumeOf, this.getRateOf, this.emitter);
  }
}
