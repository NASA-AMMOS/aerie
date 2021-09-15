package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.Objects;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public class SampledResource<T> implements DiscreteResource<T> {
  private final Register<T> result;
  private final Supplier<T> sampler;

  public SampledResource(final Supplier<T> sampler) {
    this.result = Register.create(sampler.get());
    this.sampler = Objects.requireNonNull(sampler);

    spawn(this::takeSamples);
  }

  private void takeSamples() {
    while (true) {
      var sample = sampler.get();
      if (!result.get().equals(sample)) {
        result.set(sample);
      }
      delay(1, SECOND);
    }
  }

  @Override
  public T getDynamics() {
    return this.result.getDynamics();
  }

  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
