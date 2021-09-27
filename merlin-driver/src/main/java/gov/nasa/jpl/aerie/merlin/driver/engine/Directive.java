package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/** A directive to a specific mission model. */
// TODO: Move this into the framework basement layer.
public final record Directive<Model, DirectiveType> (
    TaskSpecType<Model, DirectiveType> directiveType,
    DirectiveType directive
) {
  public static <$Schema, DirectiveType> Directive<$Schema, DirectiveType>
  instantiate(final @Nullable TaskSpecType<$Schema, DirectiveType> directiveType, final Map<String, SerializedValue> arguments)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    if (directiveType == null) throw new TaskSpecType.UnconstructableTaskSpecException();
    return new Directive<>(directiveType, directiveType.instantiate(arguments));
  }

  public String getType() {
    return this.directiveType.getName();
  }

  public Map<String, SerializedValue> getArguments() {
    return this.directiveType.getArguments(this.directive);
  }

  public <$Schema, $Timeline extends $Schema>
  Task<$Timeline> createTask(final Phantom<$Schema, Model> model) {
    return this.directiveType.createTask(model, this.directive);
  }

  private @interface Nullable {}
}
