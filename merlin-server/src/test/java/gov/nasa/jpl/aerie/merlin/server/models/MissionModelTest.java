package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
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

  private MissionModelFacade.Unconfigured<?> unconfiguredMissionModel;

    @BeforeEach
    public void initialize() {
        this.unconfiguredMissionModel = new MissionModelFacade.Unconfigured<>(new GeneratedMissionModelFactory());
    }

    @AfterEach
    public void teardown() {
      // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    }

    @Test
    public void shouldGetActivityTypeList() {
        // GIVEN
      final Map<String, ActivityType> expectedTypes = Map.of(
            "foo", new ActivityType(
                "foo",
                List.of(
                    new Parameter("x", ValueSchema.INT),
                    new Parameter("y", ValueSchema.STRING),
                    new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))),
                List.of(),
                new EnumValueMapper<>(Unit.class).getValueSchema()
            ));

        // WHEN
        final Map<String, ActivityType> typeList = unconfiguredMissionModel.getActivityTypes();

        // THEN
        assertThat(typeList).containsAllEntriesOf(expectedTypes);
    }

    @Test
    public void shouldGetActivityType() throws MissionModelFacade.NoSuchActivityTypeException {
        // GIVEN
      final ActivityType expectedType = new ActivityType(
            "foo",
            List.of(
                new Parameter("x", ValueSchema.INT),
                new Parameter("y", ValueSchema.STRING),
                new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))),
            List.of(),
            new EnumValueMapper<>(Unit.class).getValueSchema()
        );

        // WHEN
        final ActivityType type = unconfiguredMissionModel.getActivityType(expectedType.name());

        // THEN
        assertThat(type).isEqualTo(expectedType);
    }

    @Test
    public void shouldNotGetActivityTypeForNonexistentActivityType() {
        // GIVEN
        final String activityId = "nonexistent activity type";

        // WHEN
        final Throwable thrown = catchThrowable(() -> unconfiguredMissionModel.getActivityType(activityId));

        // THEN
        assertThat(thrown).isInstanceOf(MissionModelFacade.NoSuchActivityTypeException.class);
    }

    @Test
    public void shouldInstantiateActivityInstance()
    throws MissionModelFacade.NoSuchActivityTypeException, InvalidArgumentsException
    {
        // GIVEN
        final var typeName = "foo";
        final var parameters = new HashMap<>(Map.of("x", SerializedValue.of(0),
                                                    "y", SerializedValue.of("test")));

        // WHEN
        final var failures = unconfiguredMissionModel.validateActivityArguments(new SerializedActivity(typeName, parameters));

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
        final Throwable thrown = catchThrowable(() -> unconfiguredMissionModel.validateActivityArguments(new SerializedActivity(typeName, parameters)));

        // THEN
        assertThat(thrown).isInstanceOf(InvalidArgumentsException.class);
    }

    @Test
    public void shouldNotInstantiateActivityInstanceWithExtraParameter() {
        // GIVEN
        final var typeName = "foo";
        final var parameters = new HashMap<>(Map.of("Nonexistent", SerializedValue.of("")));

        // WHEN
        final Throwable thrown = catchThrowable(() -> unconfiguredMissionModel.validateActivityArguments(new SerializedActivity(typeName, parameters)));

        // THEN
        assertThat(thrown).isInstanceOf(InvalidArgumentsException.class);
    }
}
