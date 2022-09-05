package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.framework.Scoping;
import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;

import java.util.function.Function;

public final class MerlinTestContext<UNUSED, Model> {
  private final Registrar registrar;
  private Model model = null;
  private Scoping<Model> scoping = $ -> () -> {};

  public MerlinTestContext(final Registrar registrar) {
    this.registrar = registrar;
  }

  public void use(final Model model, final Function<DirectiveTypeRegistrar<RootModel<Model>>, ? extends Scoping<Model>> scope) {
    this.model = model;

    // Don't bother storing the directive types -- we don't care about them! We just want the `Scoping` return value.
    this.scoping = scope.apply(new DirectiveTypeRegistrar<>() {
      @Override
      public <Input, Output>
      void registerDirectiveType(final String name, final DirectiveType<RootModel<Model>, Input, Output> directiveType) {
      }
    });
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
