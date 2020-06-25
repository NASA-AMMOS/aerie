package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataBin;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DataBinResource {
  private final Function<String, Double> getVolumeOf;
  private final Function<String, Double> getRateOf;
  private final Consumer<Event> emitter;
  private final String binName;

  public DataBinResource(final String binName, final Function<String, Double> getVolumeOf, final Function<String, Double> getRateOf, final Consumer<Event> emitter) {
    this.getVolumeOf = getVolumeOf;
    this.getRateOf = getRateOf;
    this.emitter = emitter;
    this.binName = binName;
  }

  public final GettableResource<Double> volume = new GettableResource<>() {
    @Override
    public Double get() {
      return getVolumeOf.apply(binName);
    }

    @Override
    public String toString() {
      return this.get().toString();
    }
  };

  public final SettableCumulableResource<Double, Double> rate = new SettableCumulableResource<>() {
    @Override
    public void add(final Double delta) {
      emitter.accept(Event.addDataRate(binName, delta));
    }

    @Override
    public Double get() {
      return getRateOf.apply(binName);
    }

    @Override
    public String toString() {
      return this.get().toString();
    }
  };
}
