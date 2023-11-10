package gov.nasa.jpl.aerie.merlin.server.models;

public record MissionModelId(long id) {
  @Override
  public String toString() {
    return String.valueOf(this.id());
  }
}
