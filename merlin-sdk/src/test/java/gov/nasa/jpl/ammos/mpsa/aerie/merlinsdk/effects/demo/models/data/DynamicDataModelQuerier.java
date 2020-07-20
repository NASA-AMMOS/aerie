package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import java.util.function.Supplier;

public final class DynamicDataModelQuerier implements DataModelQuerier {
  private final Supplier<DataModelQuerier> querier;

  public DynamicDataModelQuerier(final Supplier<DataModelQuerier> querier) {
    this.querier = querier;
  }

  @Override
  public DataBinQuerier getBin(final String binName) {
    return new DynamicDataBinQuerier(() -> this.querier.get().getBin(binName));
  }
}
