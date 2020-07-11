package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import java.util.function.Supplier;

public final class DynamicDataBinQuerier implements DataBinQuerier {
  private final Supplier<DataBinQuerier> querier;

  public DynamicDataBinQuerier(final Supplier<DataBinQuerier> querier) {
    this.querier = querier;
  }

  @Override
  public double getRate() {
    return this.querier.get().getRate();
  }

  @Override
  public double getVolume() {
    return this.querier.get().getVolume();
  }
}

