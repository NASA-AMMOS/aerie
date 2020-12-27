package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states.RegisterModule;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;

public final class CumulableModule<$Schema> extends Module<$Schema> {
  private final DoubleValueMapper mapper = new DoubleValueMapper();

  public final RegisterModule<$Schema, Double> value;
  public final DiscreteResource<$Schema, Boolean> conflicted;

  public CumulableModule(
      final ResourcesBuilder.Cursor<$Schema> builder,
      final double initialValue)
  {
    super(builder);

    this.value = new RegisterModule<>(builder, initialValue, mapper);
    this.conflicted = this.value.conflicted;
  }

  public static <$Schema>
  CumulableModule<$Schema>
  create(final ResourcesBuilder.Cursor<$Schema> builder, final double initialValue) {
    return new CumulableModule<>(builder, initialValue);
  }

  public void add(final double inc) {
    this.value.set(this.value.get() + inc);
  }

  public void subtract(final double inc) {
    this.value.set(this.value.get() - inc);
  }
}
