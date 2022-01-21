package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;

public record ActivityTypeRecord(
    String name,
    TypeElement declaration,
    List<ParameterRecord> parameters,
    List<ParameterValidationRecord> validations,
    MapperRecord mapper,
    ExportDefaultsStyle defaultsStyle,
    Optional<Pair<String, ActivityType.Executor>> effectModel
  ) implements ExportTypeRecord { }
