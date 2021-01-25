package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.Objects;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.time.Duration.SECOND;

public class SampledResource<T> extends Model implements DiscreteResource<T> {
  private final Register<T> result;
  private final Supplier<T> sampler;

  public SampledResource(final Registrar builder, final Supplier<T> sampler, final T initialValue) {
    super(builder);
    this.result = Register.create(builder, initialValue);
    this.sampler = Objects.requireNonNull(sampler);
    builder.daemon("results", this::takeSamples);
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
}
