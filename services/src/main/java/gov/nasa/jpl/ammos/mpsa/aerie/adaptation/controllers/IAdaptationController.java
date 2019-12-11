package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationContractException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Stream;

public interface IAdaptationController {
    Stream<Pair<String, Adaptation>> getAdaptations();
    Adaptation getAdaptationById(String adaptationId)
        throws NoSuchAdaptationException;
    String addAdaptation(NewAdaptation adaptation)
        throws ValidationException;
    void removeAdaptation(String adaptationId)
        throws NoSuchAdaptationException;
    Map<String, ActivityType> getActivityTypes(String adaptationId)
        throws NoSuchAdaptationException, AdaptationContractException;
    ActivityType getActivityType(String adaptationId, String activityTypeId)
        throws NoSuchAdaptationException, AdaptationContractException, NoSuchActivityTypeException;
    Activity<?> instantiateActivity(final String adaptationId, final SerializedActivity activityParameters)
        throws NoSuchAdaptationException, AdaptationContractException, NoSuchActivityTypeException,
        UnconstructableActivityInstanceException;
}
