package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class AdaptationTest {
    private AdaptationFacade<?> adaptation;

    @Before
    public void initialize() throws AdaptationFacade.AdaptationContractException {
        this.adaptation = new AdaptationFacade<>(new GeneratedAdaptationFactory().instantiate());
    }

    @Test
    public void shouldGetActivityTypeList() throws AdaptationFacade.AdaptationContractException {
        // GIVEN
        final Map<String, ActivityType> expectedTypes = Map.of(
            "foo", new ActivityType(
                "foo",
                Map.of(
                    "x", ValueSchema.INT,
                    "y", ValueSchema.STRING),
                Map.of(
                    "x", SerializedValue.of(0),
                    "y", SerializedValue.of("test"))));

        // WHEN
        final Map<String, ActivityType> typeList = adaptation.getActivityTypes();

        // THEN
        assertThat(typeList).containsAllEntriesOf(expectedTypes);
    }

    @Test
    public void shouldGetActivityType() throws AdaptationFacade.NoSuchActivityTypeException, AdaptationFacade.AdaptationContractException {
        // GIVEN
        final ActivityType expectedType = new ActivityType(
            "foo",
            Map.of(
                "x", ValueSchema.INT,
                "y", ValueSchema.STRING),
            Map.of(
                "x", SerializedValue.of(0),
                "y", SerializedValue.of("test")));

        // WHEN
        final ActivityType type = adaptation.getActivityType(expectedType.name);

        // THEN
        assertThat(type).isEqualTo(expectedType);
    }

    @Test
    public void shouldNotGetActivityTypeForNonexistentActivityType() {
        // GIVEN
        final String activityId = "nonexistent activity type";

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
        final var typeName = "foo";
        final var parameters = new HashMap<>(Map.of("x", SerializedValue.of(0),
                                                    "y", SerializedValue.of("test")));

        // WHEN
        final var failures = adaptation.validateActivity(typeName, parameters);

        // THEN
        assertThat(failures).isEmpty();
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithIncorrectParameterType() {
        // GIVEN
        final var typeName = "foo";
        final var parameters = new HashMap<>(Map.of("x", SerializedValue.of(0),
                                                    "y", SerializedValue.of(1.0)));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.validateActivity(typeName, parameters));

        // THEN
        assertThat(thrown).isInstanceOf(AdaptationFacade.UnconstructableActivityInstanceException.class);
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithExtraParameter() {
        // GIVEN
        final var typeName = "foo";
        final var parameters = new HashMap<>(Map.of("Nonexistent", SerializedValue.of("")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.validateActivity(typeName, parameters));

        // THEN
        assertThat(thrown).isInstanceOf(AdaptationFacade.UnconstructableActivityInstanceException.class);
    }
}
