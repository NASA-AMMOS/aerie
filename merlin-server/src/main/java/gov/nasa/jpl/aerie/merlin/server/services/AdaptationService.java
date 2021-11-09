package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.AdaptationLoader;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;

import java.util.List;
import java.util.Map;

public interface AdaptationService {
  Map<String, AdaptationJar> getAdaptations();

  AdaptationJar getAdaptationById(String adaptationId)
  throws NoSuchAdaptationException;

  Map<String, Constraint> getConstraints(String adaptationId)
  throws NoSuchAdaptationException;

  Map<String, ValueSchema> getStatesSchemas(String adaptationId)
  throws NoSuchAdaptationException;
  Map<String, ActivityType> getActivityTypes(String adaptationId)
  throws NoSuchAdaptationException;
  // TODO: Provide a finer-scoped validation return type. Mere strings make all validations equally severe.
  List<String> validateActivityParameters(String adaptationId, SerializedActivity activityParameters)
  throws NoSuchAdaptationException;

  Map<String, SerializedValue> getActivityEffectiveArguments(String adaptationId, SerializedActivity activity)
  throws NoSuchAdaptationException,
    NoSuchActivityTypeException,
    UnconstructableActivityInstanceException,
    MissingArgumentException;

  List<Parameter> getModelParameters(String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException;

  SimulationResults runSimulation(CreateSimulationMessage message)
  throws NoSuchAdaptationException, AdaptationFacade.NoSuchActivityTypeException;

  void refreshModelParameters(String adaptationId) throws NoSuchAdaptationException;
  void refreshActivityTypes(String adaptationId) throws NoSuchAdaptationException;

  class AdaptationRejectedException extends Exception {
    public AdaptationRejectedException(final String message) { super(message); }
  }

  class NoSuchAdaptationException extends Exception {
    private final String id;

    public NoSuchAdaptationException(final String id, final Throwable cause) {
      super("No adaptation exists with id `" + id + "`", cause);
      this.id = id;
    }

    public NoSuchAdaptationException(final String id) { this(id, null); }

    public String getInvalidAdaptationId() { return this.id; }
  }

  class NoSuchActivityTypeException extends Exception {
    public final String activityTypeId;

    public NoSuchActivityTypeException(final String activityTypeId, final Throwable cause) {
      super(cause);
      this.activityTypeId = activityTypeId;
    }

    public NoSuchActivityTypeException(final String activityTypeId) { this(activityTypeId, null); }
  }

  class UnconstructableActivityInstanceException extends Exception {
    public final String activityTypeId;

    public UnconstructableActivityInstanceException(final String activityTypeId, final Throwable cause) {
      super(cause);
      this.activityTypeId = activityTypeId;
    }
  }
}
