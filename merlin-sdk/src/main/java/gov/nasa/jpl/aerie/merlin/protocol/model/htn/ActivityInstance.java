package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

public record ActivityInstance(ActivityReference reference) {
  public static ActivityInstance of(ActivityReference reference){
    return new ActivityInstance(reference);
  }
}
