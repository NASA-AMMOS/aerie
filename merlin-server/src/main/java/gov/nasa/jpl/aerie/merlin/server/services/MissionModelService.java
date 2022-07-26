package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;

import java.util.List;
import java.util.Map;

public interface MissionModelService {
  Map<String, MissionModelJar> getMissionModels();

  MissionModelJar getMissionModelById(String missionModelId)
  throws NoSuchMissionModelException;

  Map<String, Constraint> getConstraints(String missionModelId)
  throws NoSuchMissionModelException;

  Map<String, ValueSchema> getStatesSchemas(String missionModelId)
  throws NoSuchMissionModelException;

  /**
   * getActivityTypes uses the cached result of refreshActivityTypes. For this reason, refreshActivityTypes
   * should be called first.
   */
  Map<String, ActivityType> getActivityTypes(String missionModelId)
  throws NoSuchMissionModelException;
  // TODO: Provide a finer-scoped validation return type. Mere strings make all validations equally severe.
  List<String> validateActivityArguments(String missionModelId, SerializedActivity activity)
  throws NoSuchMissionModelException, InvalidArgumentsException;

  Map<ActivityInstanceId, String> validateActivityInstantiations(String missionModelId, Map<ActivityInstanceId, SerializedActivity> activities)
  throws NoSuchMissionModelException, LocalMissionModelService.MissionModelLoadException;

  Map<String, SerializedValue> getActivityEffectiveArguments(String missionModelId, SerializedActivity activity)
  throws NoSuchMissionModelException,
         NoSuchActivityTypeException,
         InvalidArgumentsException;

  List<String> validateModelArguments(String missionModelId, Map<String, SerializedValue> arguments)
  throws NoSuchMissionModelException,
         LocalMissionModelService.MissionModelLoadException,
         InvalidArgumentsException;

  List<Parameter> getModelParameters(String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoader.MissionModelLoadException;

  Map<String, SerializedValue> getModelEffectiveArguments(String missionModelId, Map<String, SerializedValue> arguments)
  throws NoSuchMissionModelException,
         LocalMissionModelService.MissionModelLoadException,
         InvalidArgumentsException;

  SimulationResults runSimulation(CreateSimulationMessage message)
          throws NoSuchMissionModelException, MissionModelFacade.NoSuchActivityTypeException;

  void refreshModelParameters(String missionModelId) throws NoSuchMissionModelException;
  void refreshActivityTypes(String missionModelId) throws NoSuchMissionModelException;

  final class NoSuchMissionModelException extends Exception {
    public final String missionModelId;

    public NoSuchMissionModelException(final String missionModelId, final Throwable cause) {
      super("No mission model exists with id `" + missionModelId + "`", cause);
      this.missionModelId = missionModelId;
    }

    public NoSuchMissionModelException(final String missionModelId) { this(missionModelId, null); }
  }

  final class NoSuchActivityTypeException extends Exception {
    public final String activityTypeId;

    public NoSuchActivityTypeException(final String activityTypeId, final Throwable cause) {
      super(cause);
      this.activityTypeId = activityTypeId;
    }

    public NoSuchActivityTypeException(final String activityTypeId) { this(activityTypeId, null); }
  }
}
