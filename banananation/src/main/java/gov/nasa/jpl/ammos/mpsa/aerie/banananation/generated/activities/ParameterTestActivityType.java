package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.ParameterTestActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.ParameterTestActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

import java.util.List;
import java.util.Map;

public class ParameterTestActivityType<$Schema> implements TaskSpecType<$Schema, ParameterTestActivity> {
  private final ParameterTestActivityMapper mapper = new ParameterTestActivityMapper();

  private final DynamicCell<Context<$Schema>> rootContext;
  private final BanananationResources<$Schema> container;

  public ParameterTestActivityType(
      final DynamicCell<Context<$Schema>> rootContext,
      final BanananationResources<$Schema> container)
  {
    this.rootContext = rootContext;
    this.container = container;
  }

  @Override
  public String getName() {
    return this.mapper.getName();
  }

  @Override
  public Map<String, ValueSchema> getParameters() {
    return this.mapper.getParameters();
  }

  @Override
  public ParameterTestActivity instantiateDefault() {
    return this.mapper.instantiateDefault();
  }

  @Override
  public ParameterTestActivity instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException
  {
    return this.mapper.instantiate(arguments);
  }

  @Override
  public Map<String, SerializedValue> getArguments(final ParameterTestActivity activity) {
    return this.mapper.getArguments(activity);
  }

  @Override
  public List<String> getValidationFailures(final ParameterTestActivity activity) {
    return this.mapper.getValidationFailures(activity);
  }

  @Override
  public <$Timeline extends $Schema> Task<$Timeline> createTask(final ParameterTestActivity activity) {
    return $ -> TaskStatus.completed();
  }
}
