package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;

public final class AdditiveRegister extends Model {
  public final Counter<Double> value;

  public AdditiveRegister(final Registrar registrar, final double initialValue) {
    super(registrar);

    this.value = Counter.ofDouble(registrar, initialValue);
  }

  public static
  AdditiveRegister
  create(final Registrar registrar, final double initialValue) {
    return new AdditiveRegister(registrar, initialValue);
  }

  public void add(final double inc) {
    this.value.add(inc);
  }

  public void subtract(final double inc) {
    this.value.add(-inc);
  }
}
