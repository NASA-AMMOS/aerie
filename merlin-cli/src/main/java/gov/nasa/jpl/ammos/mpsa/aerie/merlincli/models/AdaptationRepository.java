package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import java.io.File;

public interface AdaptationRepository {
    String createAdaptation(Adaptation adaptation, File adaptationJar) throws InvalidAdaptationException;

    class InvalidAdaptationException extends Exception {}
}
