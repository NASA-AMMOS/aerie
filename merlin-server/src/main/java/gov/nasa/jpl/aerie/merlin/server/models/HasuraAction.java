package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record HasuraAction<I extends HasuraAction.Input>(String name, I input, Session session)
{
  public record Session(String hasuraRole, String hasuraUserId) { }

  public sealed interface Input { }

  public record MissionModelInput(String missionModelId) implements Input { }
  public record PlanInput(PlanId planId) implements Input { }
  public record ConstraintViolationsInput(PlanId planId, Optional<SimulationDatasetId> simulationDatasetId) implements Input { }
  public record ActivityInput(String missionModelId,
                              String activityTypeName,
                              Map<String, SerializedValue> arguments) implements Input {}
  public record ActivityBulkInput(String missionModelId, List<SerializedActivity> activities) implements Input {}
  public record MissionModelArgumentsInput(String missionModelId,
                              Map<String, SerializedValue> arguments) implements Input {}
  public record UploadExternalDatasetInput(PlanId planId,
                                           Optional<SimulationDatasetId> simulationDatasetId,
                                           Timestamp datasetStart,
                                           ProfileSet profileSet) implements Input {}
  public record ExtendExternalDatasetInput(DatasetId datasetId,
                                           ProfileSet profileSet) implements Input {}

  public record ConstraintsInput(String missionModelId, Optional<PlanId> planId) implements Input {}
}
