package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

public final class StubAdaptationController implements App {
    public static final String EXISTENT_ADAPTATION_ID = "abc";
    public static final String NONEXISTENT_ADAPTATION_ID = "def";
    public static final AdaptationJar EXISTENT_ADAPTATION;
    public static final Map<Object, Object> VALID_NEW_ADAPTATION;
    public static final Map<Object, Object> INVALID_NEW_ADAPTATION;

    public static final String EXISTENT_ACTIVITY_TYPE = "activity";
    public static final String NONEXISTENT_ACTIVITY_TYPE = "no-activity";
    public static final ActivityType EXISTENT_ACTIVITY = new ActivityType(
        EXISTENT_ACTIVITY_TYPE,
        Map.of("Param", ParameterSchema.STRING),
        Map.of("Param", SerializedParameter.of("Default")));

    public static final SerializedActivity VALID_ACTIVITY_INSTANCE = new SerializedActivity(
        EXISTENT_ACTIVITY_TYPE,
        Map.of("Param", SerializedParameter.of("Value")));
    public static final SerializedActivity INVALID_ACTIVITY_INSTANCE = new SerializedActivity(
        EXISTENT_ACTIVITY_TYPE,
        Map.of("Param", SerializedParameter.of("")));
    public static final SerializedActivity UNCONSTRUCTABLE_ACTIVITY_INSTANCE = new SerializedActivity(
        EXISTENT_ACTIVITY_TYPE,
        Map.of("Nonexistent", SerializedParameter.of("Value")));
    public static final SerializedActivity NONEXISTENT_ACTIVITY_INSTANCE = new SerializedActivity(
        NONEXISTENT_ACTIVITY_TYPE,
        Map.of());

    public static final List<String> INVALID_ACTIVITY_INSTANCE_FAILURES = List.of("just wrong");

    static {
        VALID_NEW_ADAPTATION = new HashMap<>();
        VALID_NEW_ADAPTATION.put("name", "adaptation");
        VALID_NEW_ADAPTATION.put("version", "1.0");
        VALID_NEW_ADAPTATION.put("file", Fixtures.banananation);

        INVALID_NEW_ADAPTATION = new HashMap<>();
        INVALID_NEW_ADAPTATION.put("name", "adaptation");
        INVALID_NEW_ADAPTATION.put("version", "FAILFAILFAILFAILFAIL");
        INVALID_NEW_ADAPTATION.put("mission","mission");
        INVALID_NEW_ADAPTATION.put("file", Fixtures.banananation);

        EXISTENT_ADAPTATION = new AdaptationJar();
        EXISTENT_ADAPTATION.name = "adaptation";
        EXISTENT_ADAPTATION.version = "1.0a";
        EXISTENT_ADAPTATION.mission = "mission";
        EXISTENT_ADAPTATION.owner = "Tester";
        EXISTENT_ADAPTATION.path = Fixtures.banananation;
    }

    @Override
    public Map<String, AdaptationJar> getAdaptations() {
        return Map.of(EXISTENT_ADAPTATION_ID, EXISTENT_ADAPTATION);

    }

    @Override
    public AdaptationJar getAdaptationById(final String adaptationId) throws NoSuchAdaptationException {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }

        return EXISTENT_ADAPTATION;
    }

    @Override
    public String addAdaptation(final NewAdaptation adaptation) throws ValidationException {
        if (adaptation.version.equals("FAILFAILFAILFAILFAIL")) {
            throw new ValidationException("invalid new adaptation", List.of("an error"));
        }

        return EXISTENT_ADAPTATION_ID;
    }

    @Override
    public void removeAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }
    }

    @Override
    public Map<String, ActivityType> getActivityTypes(final String adaptationId) throws NoSuchAdaptationException {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }

        return Map.of(EXISTENT_ACTIVITY_TYPE, EXISTENT_ACTIVITY);
    }

    @Override
    public ActivityType getActivityType(final String adaptationId, final String activityType) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }

        if (!Objects.equals(activityType, EXISTENT_ACTIVITY_TYPE)) {
            throw new NoSuchActivityTypeException(adaptationId, activityType);
        }

        return EXISTENT_ACTIVITY;
    }

    @Override
    public List<String> validateActivityParameters(final String adaptationId, final SerializedActivity activityParameters)
        throws NoSuchAdaptationException, NoSuchActivityTypeException, UnconstructableActivityInstanceException
    {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }

        if (Objects.equals(activityParameters, NONEXISTENT_ACTIVITY_INSTANCE)) {
            throw new NoSuchActivityTypeException(adaptationId, activityParameters.getTypeName());
        } else if (Objects.equals(activityParameters, UNCONSTRUCTABLE_ACTIVITY_INSTANCE)) {
            throw new UnconstructableActivityInstanceException("Unconstructable activity instance");
        }

        if (Objects.equals(activityParameters, INVALID_ACTIVITY_INSTANCE)) {
            return INVALID_ACTIVITY_INSTANCE_FAILURES;
        } else {
            return Collections.emptyList();
        }
    }
}
