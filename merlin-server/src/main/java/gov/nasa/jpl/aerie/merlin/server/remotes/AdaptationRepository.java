package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;

import java.util.List;
import java.util.Map;

public interface AdaptationRepository {
    // Queries
    Map<String, AdaptationJar> getAllAdaptations();
    AdaptationJar getAdaptation(String id) throws NoSuchAdaptationException;
    Map<String, Constraint> getConstraints(String adaptationId) throws NoSuchAdaptationException;

    // Mutations
    String createAdaptation(AdaptationJar adaptationJar);
    void updateAdaptationDerivedData(String adaptationId, final List<Parameter> modelParameters, final Map<String, ActivityType> activityTypes) throws NoSuchAdaptationException;
    void deleteAdaptation(String adaptationId) throws NoSuchAdaptationException;
    void replaceAdaptationConstraints(String adaptationId, Map<String, Constraint> constraints) throws NoSuchAdaptationException;
    void deleteAdaptationConstraint(String adaptationId, String constraintId) throws NoSuchAdaptationException;

    class NoSuchAdaptationException extends Exception {}
}
