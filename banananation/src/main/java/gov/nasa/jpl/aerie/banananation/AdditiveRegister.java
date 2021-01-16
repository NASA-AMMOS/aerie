package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

public final class AdditiveRegister extends Model {
  public final Register<Double> value;
  public final DiscreteResource<Boolean> conflicted;

  public AdditiveRegister(final Registrar<?> registrar, final double initialValue) {
    super(registrar);

    this.value = Register.create(registrar, initialValue);
    this.conflicted = this.value.conflicted;
  }

  public static
  AdditiveRegister
  create(final Registrar<?> registrar, final double initialValue) {
    return new AdditiveRegister(registrar, initialValue);
  }

  public void add(final double inc) {
    this.value.set(this.value.get() + inc);
  }

  public void subtract(final double inc) {
    this.value.set(this.value.get() - inc);
  }
}
