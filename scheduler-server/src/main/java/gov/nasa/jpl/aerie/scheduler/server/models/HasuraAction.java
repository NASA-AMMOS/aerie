package gov.nasa.jpl.aerie.scheduler.server.models;

public record HasuraAction<I extends HasuraAction.Input>(String name, I input, Session session)
{
  public record Session(String hasuraRole, String hasuraUserId) { }

  public sealed interface Input permits SpecificationInput { }

  public record SpecificationInput(SpecificationId specificationId) implements Input { }
}
