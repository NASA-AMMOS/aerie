package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;

import java.util.Map;

public interface AdaptationRepository {
    // Queries
    Map<String, AdaptationJar> getAllAdaptations();
    AdaptationJar getAdaptation(String id) throws NoSuchAdaptationException;
    Map<String, Constraint> getConstraints(String adaptationId) throws NoSuchAdaptationException;

    // Mutations
    String createAdaptation(AdaptationJar adaptationJar);
    void deleteAdaptation(String adaptationId) throws NoSuchAdaptationException;
    void replaceConstraints(String adaptationId, Map<String, Constraint> constraints) throws NoSuchAdaptationException;
    void deleteConstraint(String adaptationId, String constraintId) throws NoSuchAdaptationException;

    class NoSuchAdaptationException extends Exception {}
}
