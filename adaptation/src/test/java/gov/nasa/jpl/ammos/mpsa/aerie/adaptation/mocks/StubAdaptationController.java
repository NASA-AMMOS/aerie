package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.IAdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

public final class StubAdaptationController implements IAdaptationController {
    public static final String EXISTENT_ADAPTATION_ID = "abc";
    public static final String NONEXISTENT_ADAPTATION_ID = "def";
    public static final Adaptation EXISTENT_ADAPTATION;
    public static final Map<Object, Object> VALID_NEW_ADAPTATION;
    public static final Map<Object, Object> INVALID_NEW_ADAPTATION;

    public static final String EXISTENT_ACTIVITY_ID = "activity";
    public static final String NONEXISTENT_ACTIVITY_ID = "no-activity";
    public static final ActivityType EXISTENT_ACTIVITY = new ActivityType(
        EXISTENT_ACTIVITY_ID,
        Map.of("Param", ParameterSchema.STRING));

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

        EXISTENT_ADAPTATION = new Adaptation();
        EXISTENT_ADAPTATION.name = "adaptation";
        EXISTENT_ADAPTATION.version = "1.0a";
        EXISTENT_ADAPTATION.mission = "mission";
        EXISTENT_ADAPTATION.owner = "Tester";
        EXISTENT_ADAPTATION.path = Fixtures.banananation;
    }

    @Override
    public Stream<Pair<String, Adaptation>> getAdaptations() {
        return Stream.of(Pair.of(EXISTENT_ADAPTATION_ID, EXISTENT_ADAPTATION));

    }

    @Override
    public Adaptation getAdaptationById(final String adaptationId) throws NoSuchAdaptationException {
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

        return Map.of(EXISTENT_ACTIVITY_ID, EXISTENT_ACTIVITY);
    }

    @Override
    public ActivityType getActivityType(final String adaptationId, final String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }

        if (!Objects.equals(activityTypeId, EXISTENT_ACTIVITY_ID)) {
            throw new NoSuchActivityTypeException(adaptationId, activityTypeId);
        }

        return EXISTENT_ACTIVITY;
    }

    @Override
    public Map<String, ParameterSchema> getActivityTypeParameters(final String adaptationId, final String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
            throw new NoSuchAdaptationException(adaptationId);
        }

        if (!Objects.equals(activityTypeId, EXISTENT_ACTIVITY_ID)) {
            throw new NoSuchActivityTypeException(adaptationId, activityTypeId);
        }

        return new HashMap<>(EXISTENT_ACTIVITY.parameters);
    }
}
