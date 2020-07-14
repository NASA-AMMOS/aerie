package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataBinQuerier;

import java.util.function.Consumer;

public final class DataBinResource {
  private final DataBinQuerier querier;
  private final Consumer<Double> emitter;

  public DataBinResource(final DataBinQuerier querier, final Consumer<Double> emitter) {
    this.querier = querier;
    this.emitter = emitter;
  }

  public final GettableResource<Double> volume = new GettableResource<>() {
    @Override
    public Double get() {
      return querier.getVolume();
    }

    @Override
    public String toString() {
      return this.get().toString();
    }
  };

  public final SettableCumulableResource<Double, Double> rate = new SettableCumulableResource<>() {
    @Override
    public void add(final Double delta) {
      emitter.accept(delta);
    }

    @Override
    public Double get() {
      return querier.getRate();
    }

    @Override
    public String toString() {
      return this.get().toString();
    }
  };
}
