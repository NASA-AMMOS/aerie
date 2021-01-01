package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;

import java.util.Objects;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.time.Duration.SECOND;

public class SampledResource<$Schema, T> extends Model<$Schema> {
  private final Register<$Schema, T> result;
  private final Supplier<T> sampler;

  public SampledResource(
      final Registrar<$Schema> builder,
      final Supplier<T> sampler,
      final T initialValue,
      final ValueMapper<T> mapper)
  {
    super(builder);
    this.result = new Register<>(builder, initialValue, mapper);
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

  public T get() {
    return this.result.get();
  }
}
