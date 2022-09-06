package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;

import java.util.function.Supplier;

public final class MerlinTestContext<UNUSED, Model> {
  private final Registrar registrar;
  private Model model = null;
  private Scoped<RootModel<Model>> scoping = Scoped.create();

  public MerlinTestContext(final Registrar registrar) {
    this.registrar = registrar;
  }

  /**
   * @deprecated
   *   If you previously passed {@code ActivityTypes::register} to this method,
   *   pass {@code GeneratedMissionModelFactory.model} instead.
   */
  @Deprecated(forRemoval = true)
  public void use(final Model model, final Supplier<? extends Scoped<RootModel<Model>>> scope) {
    this.model = model;
    this.scoping = scope.get();
  }

  public void use(final Model model, final Scoped<RootModel<Model>> scope) {
    this.model = model;
    this.scoping = scope;
  }

  public Registrar registrar() {
    return registrar;
  }

  public Model model() {
    return model;
  }

  public Scoped<RootModel<Model>> scoping() {
    return scoping;
  }
}
