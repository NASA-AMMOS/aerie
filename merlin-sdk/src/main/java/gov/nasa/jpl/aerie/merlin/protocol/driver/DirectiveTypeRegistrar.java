package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

public interface DirectiveTypeRegistrar<Model> {
  <Input, Output>
  DirectiveTypeId<Input, Output>
  registerDirectiveType(
      String name,
      TaskSpecType<Model, Input, Output> taskSpecType);
}
