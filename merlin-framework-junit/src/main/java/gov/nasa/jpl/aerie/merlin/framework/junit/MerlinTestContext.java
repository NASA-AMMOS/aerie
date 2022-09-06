package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.Scoping;

import java.util.function.Supplier;

public final class MerlinTestContext<UNUSED, Model> {
  private final Registrar registrar;
  private Model model = null;
  private Scoping<Model> scoping = $ -> () -> {};

  public MerlinTestContext(final Registrar registrar) {
    this.registrar = registrar;
  }

  public void use(final Model model, final Supplier<? extends Scoping<Model>> scope) {
    this.model = model;
    this.scoping = scope.get();
  }

  public Registrar registrar() {
    return registrar;
  }

  public Model model() {
    return model;
  }

  public Scoping<Model> scoping() {
    return scoping;
  }
}
