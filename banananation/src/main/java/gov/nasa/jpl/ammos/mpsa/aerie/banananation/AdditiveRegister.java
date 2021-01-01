package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Register;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.discrete.DiscreteResource;

public final class AdditiveRegister<$Schema> extends Model<$Schema> {
  private final DoubleValueMapper mapper = new DoubleValueMapper();

  public final Register<$Schema, Double> value;
  public final DiscreteResource<$Schema, Boolean> conflicted;

  public AdditiveRegister(
      final ResourcesBuilder.Cursor<$Schema> builder,
      final double initialValue)
  {
    super(builder);

    this.value = new Register<>(builder, initialValue, mapper);
    this.conflicted = this.value.conflicted;
  }

  public static <$Schema>
  AdditiveRegister<$Schema>
  create(final ResourcesBuilder.Cursor<$Schema> builder, final double initialValue) {
    return new AdditiveRegister<>(builder, initialValue);
  }

  public void add(final double inc) {
    this.value.set(this.value.get() + inc);
  }

  public void subtract(final double inc) {
    this.value.set(this.value.get() - inc);
  }
}
