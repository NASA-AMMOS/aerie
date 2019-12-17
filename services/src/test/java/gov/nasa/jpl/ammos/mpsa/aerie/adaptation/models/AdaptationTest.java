package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class AdaptationTest {
    private final Fixtures fixtures = new Fixtures();
    private Adaptation adaptation;

    @BeforeEach
    public void initialize() throws AdaptationRepository.NoSuchAdaptationException, Adaptation.AdaptationContractException, AdaptationLoader.AdaptationLoadException {
        final AdaptationJar adaptationJar = fixtures.adaptationRepository.getAdaptation(fixtures.EXISTENT_ADAPTATION_ID);
        final MerlinAdaptation<?> rawAdaptation = AdaptationLoader.loadAdaptation(adaptationJar.path);

        this.adaptation = new Adaptation(rawAdaptation);
    }

    @Test
    public void shouldGetActivityTypeList() throws Adaptation.AdaptationContractException {
        // GIVEN
        final Map<String, ActivityType> expectedTypes = fixtures.ACTIVITY_TYPES;

        // WHEN
        final Map<String, ActivityType> typeList = adaptation.getActivityTypes();

        // THEN
        assertThat(typeList).isEqualTo(expectedTypes);
    }

    @Test
    public void shouldGetActivityType() throws NoSuchActivityTypeException, Adaptation.AdaptationContractException {
        // GIVEN
        final String activityId = Fixtures.EXISTENT_ACTIVITY_TYPE_ID;
        final ActivityType expectedType = fixtures.ACTIVITY_TYPES.get(activityId);

        // WHEN
        final ActivityType type = adaptation.getActivityType(activityId);

        // THEN
        assertThat(type).isEqualTo(expectedType);
    }

    @Test
    public void shouldNotGetActivityTypeForNonexistentActivityType() {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.getActivityType(activityId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchActivityTypeException.class);

        final String invalidActivityId = ((NoSuchActivityTypeException)thrown).getInvalidActivityTypeId();
        assertThat(invalidActivityId).isEqualTo(activityId);
    }

    @Test
    public void shouldInstantiateActivityInstance()
        throws NoSuchActivityTypeException, Adaptation.AdaptationContractException, UnconstructableActivityInstanceException
    {
        // GIVEN
        final SerializedActivity serializedActivity = new SerializedActivity(
            "BiteBanana",
            Map.of("biteSize", SerializedParameter.of(1.0)));

        // WHEN
        final Activity<?> activityInstance = adaptation.instantiateActivity(serializedActivity);

        // THEN
        assertThat(activityInstance).isNotNull();
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithIncorrectParameterType() {
        // GIVEN
        final SerializedActivity serializedActivity = new SerializedActivity(
            "BiteBanana",
            Map.of("biteSize", SerializedParameter.of("a string!?")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.instantiateActivity(serializedActivity));

        // THEN
        assertThat(thrown).isInstanceOf(UnconstructableActivityInstanceException.class);
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithExtraParameter() {
        // GIVEN
        final SerializedActivity serializedActivity = new SerializedActivity(
            "BiteBanana",
            Map.of("Nonexistent", SerializedParameter.of("")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.instantiateActivity(serializedActivity));

        // THEN
        assertThat(thrown).isInstanceOf(UnconstructableActivityInstanceException.class);
    }
}
