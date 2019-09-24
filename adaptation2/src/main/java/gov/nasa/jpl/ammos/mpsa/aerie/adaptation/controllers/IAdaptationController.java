package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityTypeParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Stream;

public interface IAdaptationController {
    Stream<Pair<String, Adaptation>> getAdaptations();
    Adaptation getAdaptationById(String adaptationId) throws NoSuchAdaptationException;
    String addAdaptation(NewAdaptation adaptation) throws ValidationException;
    void removeAdaptation(String adaptationId) throws NoSuchAdaptationException;
    Stream<Pair<String, ActivityType>> getActivityTypes(String adaptationId) throws NoSuchAdaptationException;
    ActivityType getActivityType(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException;
    Stream<ActivityTypeParameter> getActivityTypeParameters(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException;
}
