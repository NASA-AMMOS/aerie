package gov.nasa.jpl.aerie.merlin.server.exceptions;

public class UnexpectedMissingMissionModelException extends RuntimeException {
  private final String missionModelId;

  public UnexpectedMissingMissionModelException(final String missionModelId, final Throwable cause) {
    super("MissionModel with id `" + missionModelId + "` is unexpectedly missing", cause);
    this.missionModelId = missionModelId;
  }

  public String getMissionModelId() {
    return this.missionModelId;
  }
}
