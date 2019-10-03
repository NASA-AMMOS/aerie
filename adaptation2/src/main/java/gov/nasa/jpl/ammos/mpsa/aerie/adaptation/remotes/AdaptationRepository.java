package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityTypeParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Stream;

public interface AdaptationRepository {

    // Queries
    Stream<Pair<String, Adaptation>> getAllAdaptations();
    Adaptation getAdaptation(String id) throws NoSuchAdaptationException;
    Stream<Pair<String, ActivityType>> getAllActivityTypesInAdaptation(String adaptationId) throws NoSuchAdaptationException, InvalidAdaptationJARException;
    ActivityType getActivityTypeInAdaptation(String adaptationId, String activityId) throws NoSuchAdaptationException, NoSuchActivityTypeException, InvalidAdaptationJARException;
    Stream<ActivityTypeParameter> getActivityTypeParameters(String adaptationId, String activityId) throws NoSuchAdaptationException, NoSuchActivityTypeException, InvalidAdaptationJARException;

    // Mutations
    String createAdaptation(NewAdaptation adaptation);
    void deleteAdaptation(String adaptationId) throws NoSuchAdaptationException;
}
