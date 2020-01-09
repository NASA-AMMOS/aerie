package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.AdaptationCreateFailureException;

import java.io.File;

public interface AdaptationRepository {
    String createAdaptation(Adaptation adaptation, File adaptationJar) throws AdaptationCreateFailureException;
}
