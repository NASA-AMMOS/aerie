package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.fooadaptation.Configuration;
import gov.nasa.jpl.aerie.fooadaptation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.fooadaptation.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class AdaptationTest {

    private AdaptationFacade<?> adaptation;
    private AdaptationFacade.Unconfigured<?> unconfiguredAdaptation;

    @BeforeEach
    public void initialize() throws AdaptationFacade.AdaptationContractException {
        final var configuration = new Configuration();
        final var serializedConfig = FooValueMappers.configuration().serializeValue(configuration);
        this.adaptation = makeAdaptation(new AdaptationBuilder<>(Schema.builder()), serializedConfig);
        this.unconfiguredAdaptation = new AdaptationFacade.Unconfigured<>(new GeneratedAdaptationFactory());
    }

    private static <$Schema> AdaptationFacade<$Schema> makeAdaptation(final AdaptationBuilder<$Schema> builder, final SerializedValue config) {
        final var factory = new GeneratedAdaptationFactory();
        final var model = factory.instantiate(config, builder);
        return new AdaptationFacade<>(builder.build(model, factory.getTaskSpecTypes()));
    }

    @AfterEach
    public void teardown() {
      // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    }

    @Test
    public void shouldGetActivityTypeList() throws AdaptationFacade.AdaptationContractException {
        // GIVEN
        final Map<String, ActivityType> expectedTypes = Map.of(
            "foo", new ActivityType(
                "foo",
                List.of(
                    new Parameter("x", ValueSchema.INT),
                    new Parameter("y", ValueSchema.STRING),
                    new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))),
                Map.of(
                    "x", SerializedValue.of(0),
                    "y", SerializedValue.of("test"),
                    "vecs", SerializedValue.of(
                        List.of(
                            SerializedValue.of(
                                List.of(
                                    SerializedValue.of(0.0),
                                    SerializedValue.of(0.0),
                                    SerializedValue.of(0.0))))))));
        // WHEN
        final Map<String, ActivityType> typeList = unconfiguredAdaptation.getActivityTypes();

        // THEN
        assertThat(typeList).containsAllEntriesOf(expectedTypes);
    }

    @Test
    public void shouldGetActivityType() throws AdaptationFacade.NoSuchActivityTypeException, AdaptationFacade.AdaptationContractException {
        // GIVEN
        final ActivityType expectedType = new ActivityType(
            "foo",
            List.of(
                new Parameter("x", ValueSchema.INT),
                new Parameter("y", ValueSchema.STRING),
                new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))),
            Map.of(
                "x", SerializedValue.of(0),
                "y", SerializedValue.of("test"),
                "vecs", SerializedValue.of(
                    List.of(
                        SerializedValue.of(
                            List.of(
                                SerializedValue.of(0.0),
                                SerializedValue.of(0.0),
                                SerializedValue.of(0.0)))))));

        // WHEN
        final ActivityType type = unconfiguredAdaptation.getActivityType(expectedType.name);

        // THEN
        assertThat(type).isEqualTo(expectedType);
    }

    @Test
    public void shouldNotGetActivityTypeForNonexistentActivityType() {
        // GIVEN
        final String activityId = "nonexistent activity type";

        // WHEN
        final Throwable thrown = catchThrowable(() -> unconfiguredAdaptation.getActivityType(activityId));

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
