package gov.nasa.jpl.aerie.services.plan.remotes;

import gov.nasa.jpl.aerie.services.plan.models.AdaptationJar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Stream;

public interface AdaptationRepository {
    // Queries
    Stream<Pair<String, AdaptationJar>> getAllAdaptations();
    AdaptationJar getAdaptation(String id) throws NoSuchAdaptationException;

    // Mutations
    String createAdaptation(AdaptationJar adaptationJar);
    void deleteAdaptation(String adaptationId) throws NoSuchAdaptationException;

    class NoSuchAdaptationException extends Exception {}
}
