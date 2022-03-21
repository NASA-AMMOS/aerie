package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/** A directive to a specific mission model. */
// TODO: Move this into the framework basement layer.
public record Directive<Model, DirectiveType, Return> (
    TaskSpecType<Model, DirectiveType, Return> directiveType,
    DirectiveType directive
) {
  public static <Model, DirectiveType, Return> Directive<Model, DirectiveType, Return>
  instantiate(final @Nullable TaskSpecType<Model, DirectiveType, Return> directiveType, final Map<String, SerializedValue> arguments)
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

  public Task<Return> createTask(final Model model) {
    return this.directiveType.createTask(model, this.directive);
  }

  private @interface Nullable {}
}
