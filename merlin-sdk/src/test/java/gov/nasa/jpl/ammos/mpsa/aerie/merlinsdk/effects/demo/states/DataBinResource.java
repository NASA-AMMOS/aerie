package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataBin;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DataBinResource {
  private final Supplier<DataBin> activeBin;
  private final Consumer<Event> emitter;
  private final String binName;

  public DataBinResource(final Supplier<DataBin> activeBin, final Consumer<Event> emitter, final String binName) {
    this.activeBin = activeBin;
    this.emitter = emitter;
    this.binName = binName;
  }

  public final GettableResource<Double> volume = new GettableResource<>() {
    @Override
    public Double get() {
      return activeBin.get().getVolume();
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
      return activeBin.get().getRate();
    }

    @Override
    public String toString() {
      return this.get().toString();
    }
  };
}
