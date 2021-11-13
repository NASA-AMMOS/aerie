package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;

import java.util.List;
import java.util.Map;

public interface MissionModelRepository {
    // Queries
    Map<String, MissionModelJar> getAllAdaptations();
    MissionModelJar getAdaptation(String id) throws NoSuchAdaptationException;
    Map<String, Constraint> getConstraints(String adaptationId) throws NoSuchAdaptationException;

    // Mutations
    String createAdaptation(MissionModelJar adaptationJar);
    void updateModelParameters(String adaptationId, final List<Parameter> modelParameters) throws NoSuchAdaptationException;
    void updateActivityTypes(String adaptationId, final Map<String, ActivityType> activityTypes) throws NoSuchAdaptationException;
    void deleteAdaptation(String adaptationId) throws NoSuchAdaptationException;

    class NoSuchAdaptationException extends Exception {}
}
