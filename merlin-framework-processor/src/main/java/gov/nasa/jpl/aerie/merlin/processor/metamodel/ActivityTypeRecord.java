package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ActivityTypeRecord {
  public final TypeElement declaration;
  public final String name;
  public final ActivityMapperRecord mapper;
  public final List<ActivityValidationRecord> validations;
  public final List<ActivityParameterRecord> parameters;
  public final Optional<Pair<String, ActivityType.Executor>> effectModel;
  public final ActivityDefaultsStyle activityDefaultsStyle;

  public ActivityTypeRecord(
      final TypeElement declaration,
      final String name,
      final ActivityMapperRecord mapper,
      final List<ActivityValidationRecord> validations,
      final List<ActivityParameterRecord> parameters,
      final Optional<Pair<String, ActivityType.Executor>> effectModel,
      final ActivityDefaultsStyle activityDefaultsStyle)
  {
    this.declaration = Objects.requireNonNull(declaration);
    this.name = Objects.requireNonNull(name);
    this.mapper = Objects.requireNonNull(mapper);
    this.validations = Objects.requireNonNull(validations);
    this.parameters = Objects.requireNonNull(parameters);
    this.effectModel = Objects.requireNonNull(effectModel);
    this.activityDefaultsStyle = Objects.requireNonNull(activityDefaultsStyle);
  }
}
