package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

import java.util.Map;

public final class MerlinTestContext<Model> {
  private final Registrar registrar;
  private Model model;
  private Map<String, TaskSpecType<RootModel<Model>, ?, ?>> activityTypes = Map.of();

  public MerlinTestContext(final Registrar registrar) {
    this.registrar = registrar;
  }

  public void use(final Model model, final Map<String, TaskSpecType<RootModel<Model>, ?, ?>> activityTypes) {
    this.model = model;
    this.activityTypes = activityTypes;
  }

  public Registrar registrar() {
    return registrar;
  }

  public Model model() {
    return model;
  }

  public Map<String, TaskSpecType<RootModel<Model>, ?, ?>> activityTypes() {
    return activityTypes;
  }
}
