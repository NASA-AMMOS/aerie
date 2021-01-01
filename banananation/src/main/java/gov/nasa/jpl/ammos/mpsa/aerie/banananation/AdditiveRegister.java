package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Register;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.discrete.DiscreteResource;

public final class AdditiveRegister<$Schema> extends Model<$Schema> {
  public final Register<$Schema, Double> value;
  public final DiscreteResource<$Schema, Boolean> conflicted;

  public AdditiveRegister(final Registrar<$Schema> registrar, final double initialValue) {
    super(registrar);

    this.value = Register.create(registrar, initialValue);
    this.conflicted = this.value.conflicted;
  }

  public static <$Schema>
  AdditiveRegister<$Schema>
  create(final Registrar<$Schema> registrar, final double initialValue) {
    return new AdditiveRegister<>(registrar, initialValue);
  }

  public void add(final double inc) {
    this.value.set(this.value.get() + inc);
  }

  public void subtract(final double inc) {
    this.value.set(this.value.get() - inc);
  }
}
