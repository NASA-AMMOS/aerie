package gov.nasa.jpl.aerie.merlin.framework.junit;

import java.util.Map;
import java.util.function.Function;

import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.framework.Scoping;
import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;

public final class MerlinTestContext<Registry extends Scoping<Registry, Model>, Model> {
  private final Registrar registrar;
  private Model model = null;
  private DirectiveTypeRegistry<Registry, RootModel<Registry, Model>> activityTypes = new DirectiveTypeRegistry<>(Map.of(), null);

  public MerlinTestContext(final Registrar registrar) {
    this.registrar = registrar;
  }

  public void use(final Model model, final Function<DirectiveTypeRegistrar<RootModel<Registry, Model>>, Registry> scope) {
    this.model = model;
    this.activityTypes = DirectiveTypeRegistry.extract(scope);
  }

  public Registrar registrar() {
    return registrar;
  }

  public Model model() {
    return model;
  }

  public DirectiveTypeRegistry<Registry, RootModel<Registry, Model>> activityTypes() {
    return activityTypes;
  }
}
