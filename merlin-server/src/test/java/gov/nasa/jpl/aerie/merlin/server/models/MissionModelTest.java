package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.foomissionmodel.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class MissionModelTest {

  private DirectiveTypeRegistry<?> registry;

  @BeforeEach
  public void initialize() {
      this.registry = DirectiveTypeRegistry.extract(new GeneratedModelType());
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
                  new Parameter("z", ValueSchema.INT),
                  new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))),
              List.of(),
              ValueSchema.ofStruct(Map.of())
          ));

    // WHEN
    final var activityTypes = new HashMap<String, ActivityType>();
    registry.directiveTypes().forEach((name, specType) -> activityTypes.put(
        name,
        new ActivityType(name,
                         specType.getInputType().getParameters(),
                         specType.getInputType().getRequiredParameters(),
                         specType.getOutputType().getSchema())));

    // THEN
    assertThat(activityTypes).containsAllEntriesOf(expectedTypes);
  }

  @Test
  public void shouldGetActivityType() throws MissionModelService.NoSuchActivityTypeException {
    // GIVEN
    final ActivityType expectedType = new ActivityType(
          "foo",
          List.of(
              new Parameter("x", ValueSchema.INT),
              new Parameter("y", ValueSchema.STRING),
              new Parameter("z", ValueSchema.INT),
              new Parameter("vecs", ValueSchema.ofSeries(ValueSchema.ofSeries(ValueSchema.REAL)))),
          List.of(),
          ValueSchema.ofStruct(Map.of())
      );

    // WHEN
    final var typeName = expectedType.name();
    final var specType = Optional
        .ofNullable(registry.directiveTypes().get(typeName))
        .orElseThrow(() -> new MissionModelService.NoSuchActivityTypeException(typeName));

    final var type = new ActivityType(
        typeName,
        specType.getInputType().getParameters(),
        specType.getInputType().getRequiredParameters(),
        specType.getOutputType().getSchema());

    // THEN
    assertThat(type).isEqualTo(expectedType);
  }

  @Test
  public void shouldNotGetActivityTypeForNonexistentActivityType() {
    // GIVEN
    final String activityId = "nonexistent activity type";

    // WHEN
    final var thrown = catchThrowable(() -> {
        final var specType = Optional
            .ofNullable(registry.directiveTypes().get(activityId))
            .orElseThrow(() -> new MissionModelService.NoSuchActivityTypeException(activityId));

        new ActivityType(
            activityId,
            specType.getInputType().getParameters(),
            specType.getInputType().getRequiredParameters(),
            specType.getOutputType().getSchema());
    });

    // THEN
    assertThat(thrown).isInstanceOf(MissionModelService.NoSuchActivityTypeException.class);
  }

  @Test
  public void shouldInstantiateActivityInstance()
  throws MissionModelService.NoSuchActivityTypeException, InstantiationException
  {
    // GIVEN
    final var typeName = "foo";
    final var arguments = new HashMap<>(Map.of("x", SerializedValue.of(0),
                                               "y", SerializedValue.of("test"),
                                               "z", SerializedValue.of(1)));

    // WHEN
    final var activity = new SerializedActivity(typeName, arguments);
    final var specType = Optional
        .ofNullable(registry.directiveTypes().get(activity.getTypeName()))
        .orElseThrow(() -> new MissionModelService.NoSuchActivityTypeException(activity.getTypeName()));
    final var failures = specType.getInputType().validateArguments(activity.getArguments());

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
    final var thrown = catchThrowable(() -> {
        final var activity = new SerializedActivity(typeName, parameters);
        final var specType = Optional
            .ofNullable(registry.directiveTypes().get(activity.getTypeName()))
            .orElseThrow(() -> new MissionModelService.NoSuchActivityTypeException(activity.getTypeName()));
        specType.getInputType().validateArguments(activity.getArguments());
    });

    // THEN
    assertThat(thrown).isInstanceOf(InstantiationException.class);
    if (thrown instanceof final InstantiationException e) {
      assertThat(e.extraneousArguments).isEmpty();
      assertThat(e.missingArguments).map(args -> args.parameterName()).isEqualTo(List.of("z"));
      assertThat(e.unconstructableArguments).map(args -> args.parameterName()).isEqualTo(List.of("y"));
      assertThat(e.validArguments).map(args -> args.parameterName()).isEqualTo(List.of("x", "y", "vecs"));
    }
  }

  @Test
  public void shouldNotInstantiateActivityInstanceWithExtraParameter() {
    // GIVEN
    final var typeName = "foo";
    final var parameters = new HashMap<>(Map.of("Nonexistent", SerializedValue.of("")));

    // WHEN
    final var thrown = catchThrowable(() -> {
        final var activity = new SerializedActivity(typeName, parameters);
        final var specType = Optional
            .ofNullable(registry.directiveTypes().get(activity.getTypeName()))
            .orElseThrow(() -> new MissionModelService.NoSuchActivityTypeException(activity.getTypeName()));
        specType.getInputType().validateArguments(activity.getArguments());
    });

    // THEN
    assertThat(thrown).isInstanceOf(InstantiationException.class);
    if (thrown instanceof final InstantiationException e) {
      assertThat(e.extraneousArguments).map(args -> args.parameterName()).isEqualTo(List.of("Nonexistent"));
      assertThat(e.missingArguments).map(args -> args.parameterName()).isEqualTo(List.of("z"));
      assertThat(e.unconstructableArguments).isEmpty();
      assertThat(e.validArguments).map(args -> args.parameterName()).isEqualTo(List.of("x", "y", "vecs"));
    }
  }
}
