package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.foomissionmodel.Configuration;
import gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.foomissionmodel.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class MissionModelTest {

    private MissionModelFacade<?> adaptation;
    private MissionModelFacade.Unconfigured<?> unconfiguredAdaptation;

    @BeforeEach
    public void initialize() throws MissionModelFacade.AdaptationContractException {
        final var configuration = new Configuration();
        final var serializedConfig = FooValueMappers.configuration().serializeValue(configuration);
        this.adaptation = makeAdaptation(new MissionModelBuilder<>(), serializedConfig);
        this.unconfiguredAdaptation = new MissionModelFacade.Unconfigured<>(new GeneratedMissionModelFactory());
    }

    private static <$Schema> MissionModelFacade<$Schema> makeAdaptation(final MissionModelBuilder<$Schema> builder, final SerializedValue config) {
        final var factory = new GeneratedMissionModelFactory();
        final var model = factory.instantiate(config, builder);
        return new MissionModelFacade<>(builder.build(model, factory.getTaskSpecTypes()));
    }

    @AfterEach
    public void teardown() {
      // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    }

    @Test
    public void shouldGetActivityTypeList() throws MissionModelFacade.AdaptationContractException {
        // GIVEN
        final Map<String, ActivityType> expectedTypes = Map.of(
            "foo", new ActivityType(
                "foo",
                List.of(
                    new Parameter("x", ValueSchema.INT),
                    new Parameter("y", ValueSchema.STRING),
                    new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))), List.of()));

        // WHEN
        final Map<String, ActivityType> typeList = unconfiguredAdaptation.getActivityTypes();

        // THEN
        assertThat(typeList).containsAllEntriesOf(expectedTypes);
    }

    @Test
    public void shouldGetActivityType() throws MissionModelFacade.NoSuchActivityTypeException, MissionModelFacade.AdaptationContractException {
        // GIVEN
        final ActivityType expectedType = new ActivityType(
            "foo",
            List.of(
                new Parameter("x", ValueSchema.INT),
                new Parameter("y", ValueSchema.STRING),
                new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))), List.of());

        // WHEN
        final ActivityType type = unconfiguredAdaptation.getActivityType(expectedType.name());

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
        assertThat(thrown).isInstanceOf(MissionModelFacade.NoSuchActivityTypeException.class);
    }

    @Test
    public void shouldInstantiateActivityInstance()
        throws MissionModelFacade.NoSuchActivityTypeException, MissionModelFacade.AdaptationContractException, MissionModelFacade.UnconstructableActivityInstanceException
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
        assertThat(thrown).isInstanceOf(MissionModelFacade.UnconstructableActivityInstanceException.class);
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithExtraParameter() {
        // GIVEN
        final var typeName = "foo";
        final var parameters = new HashMap<>(Map.of("Nonexistent", SerializedValue.of("")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> adaptation.validateActivity(typeName, parameters));

        // THEN
        assertThat(thrown).isInstanceOf(MissionModelFacade.UnconstructableActivityInstanceException.class);
    }
}
