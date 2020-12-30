package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.processor;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;

public final class ActivityTypeRecord {
  public final TypeElement declaration;
  public final String name;
  public final ActivityMapperRecord mapper;
  public final List<ActivityValidationRecord> validations;
  public final List<ActivityParameterRecord> parameters;
  public final ActivityExecutionType effectModel;

  public ActivityTypeRecord(
      final TypeElement declaration,
      final String name,
      final ActivityMapperRecord mapper,
      final List<ActivityValidationRecord> validations,
      final List<ActivityParameterRecord> parameters,
      final ActivityExecutionType effectModel)
  {
    this.declaration = Objects.requireNonNull(declaration);
    this.name = Objects.requireNonNull(name);
    this.mapper = Objects.requireNonNull(mapper);
    this.validations = Objects.requireNonNull(validations);
    this.parameters = Objects.requireNonNull(parameters);
    this.effectModel = Objects.requireNonNull(effectModel);
  }
}
