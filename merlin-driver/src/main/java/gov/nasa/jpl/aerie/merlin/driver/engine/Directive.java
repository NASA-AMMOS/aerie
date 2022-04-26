package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/** A directive to a specific mission model. */
// TODO: Move this into the framework basement layer.
public record Directive<Model, DirectiveType, Return> (
    TaskSpecType<Model, DirectiveType, Return> directiveType,
    String typeName,
    DirectiveType directive
) {
  public static <Model, DirectiveType, Return> Directive<Model, DirectiveType, Return>
  instantiate(final @Nullable TaskSpecType<Model, DirectiveType, Return> directiveType, final SerializedActivity instance)
  throws TaskSpecType.UnconstructableTaskSpecException, MissingArgumentsException
  {
    if (directiveType == null) throw new TaskSpecType.UnconstructableTaskSpecException();
    return new Directive<>(directiveType, instance.getTypeName(), directiveType.instantiate(instance.getArguments()));
  }

  public String getType() {
    return this.typeName;
  }

  public Map<String, SerializedValue> getArguments() {
    return this.directiveType.getArguments(this.directive);
  }

  public Task<Return> createTask(final Model model) {
    return this.directiveType.createTask(model, this.directive);
  }

  private @interface Nullable {}
}
