package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;

public interface DirectiveTypeRegistrar<Model> {
  <Input, Output>
  void registerDirectiveType(
      String name,
      DirectiveType<Model, Input, Output> directiveType);
}
