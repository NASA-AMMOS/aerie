package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;

public final class MerlinTestContext<UNUSED, Model> {
  private final Registrar registrar;
  private Model model = null;

  public MerlinTestContext(final Registrar registrar) {
    this.registrar = registrar;
  }

  public void use(final Model model) {
    this.model = model;
  }

  public Registrar registrar() {
    return registrar;
  }

  public Model model() {
    return model;
  }
}
