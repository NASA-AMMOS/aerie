package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.Objects;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.SECOND;

public class ComputedModule<$Schema, T> extends Module<$Schema> {
  private final RegisterModule<$Schema, T> result;
  private final Supplier<T> sampler;

  public ComputedModule(
      final ResourcesBuilder.Cursor<$Schema> builder,
      final Supplier<T> sampler,
      final T initialValue,
      final ValueMapper<T> mapper)
  {
    super(builder);
    this.result = new RegisterModule<>(builder, initialValue, mapper);
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
