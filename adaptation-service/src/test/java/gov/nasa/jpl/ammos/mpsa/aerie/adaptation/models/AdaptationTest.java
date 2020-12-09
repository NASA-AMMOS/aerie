package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

// Note:
// If the tests fail, please try to update the adaptation jar file.
// The latest jar file is in banananatioin/build/libs/banananation-x.x.x.jar.
// Please copy and rename the file to banananation.jar in
// adaptation-service/src/test/resources/gov/nasa/jpl/ammos/mpsa/aerie/banananation.jar
public final class AdaptationTest {
    private final Fixtures fixtures = new Fixtures();
    private AdaptationFacade<?> adaptation;

    @Before
    public void initialize() throws AdaptationRepository.NoSuchAdaptationException, AdaptationFacade.AdaptationContractException, AdaptationLoader.AdaptationLoadException {
        final AdaptationJar adaptationJar = fixtures.adaptationRepository.getAdaptation(fixtures.EXISTENT_ADAPTATION_ID);
        final MerlinAdaptation<?> rawAdaptation =
            AdaptationLoader.loadAdaptation(adaptationJar.path, adaptationJar.name, adaptationJar.version);

        this.adaptation = new AdaptationFacade<>(rawAdaptation);
    }

    @Test
    public void shouldGetActivityTypeList() throws AdaptationFacade.AdaptationContractException {
        // GIVEN
        final Map<String, ActivityType> expectedTypes = fixtures.ACTIVITY_TYPES;

        // WHEN
        final Map<String, ActivityType> typeList = adaptation.getActivityTypes();

        // THEN
        assertThat(typeList).containsAllEntriesOf(expectedTypes);
    }

    @Test
    public void shouldGetActivityType() throws AdaptationFacade.NoSuchActivityTypeException, AdaptationFacade.AdaptationContractException {
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
        final String activityId = Fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.getActivityType(activityId));

        // THEN
        assertThat(thrown).isInstanceOf(AdaptationFacade.NoSuchActivityTypeException.class);
    }

    @Test
    public void shouldInstantiateActivityInstance()
        throws AdaptationFacade.NoSuchActivityTypeException, AdaptationFacade.AdaptationContractException, AdaptationFacade.UnconstructableActivityInstanceException
    {
        // GIVEN
        final SerializedActivity serializedActivity = new SerializedActivity(
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of(1.0)));

        // WHEN
        final Activity activityInstance = adaptation.instantiateActivity(serializedActivity);

        // THEN
        assertThat(activityInstance).isNotNull();
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithIncorrectParameterType() {
        // GIVEN
        final SerializedActivity serializedActivity = new SerializedActivity(
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of("a string!?")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.instantiateActivity(serializedActivity));

        // THEN
        assertThat(thrown).isInstanceOf(AdaptationFacade.UnconstructableActivityInstanceException.class);
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithExtraParameter() {
        // GIVEN
        final SerializedActivity serializedActivity = new SerializedActivity(
            "BiteBanana",
            Map.of("Nonexistent", SerializedValue.of("")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.instantiateActivity(serializedActivity));

        // THEN
        assertThat(thrown).isInstanceOf(AdaptationFacade.UnconstructableActivityInstanceException.class);
    }
}
