package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.function.Predicate;
import java.util.function.Supplier;

public final class DynamicStateQuery<ResourceType> implements StateQuery<ResourceType> {
  private final Supplier<StateQuery<ResourceType>> querier;

  public DynamicStateQuery(final Supplier<StateQuery<ResourceType>> querier) {
    this.querier = querier;
  }

  @Override
  public ResourceType get() {
    return this.querier.get().get();
  }

  @Override
  public Windows when(final Predicate<ResourceType> condition) {
    return this.querier.get().when(condition);
  }
}
