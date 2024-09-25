package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;

public record InputTypeRecord(
    String name,
    TypeElement declaration,
    List<ParameterRecord> parameters,
    List<ParameterValidationRecord> validations,
    MapperRecord mapper,
    ExportDefaultsStyle defaultsStyle,
    Optional<Boolean> compoundShouldBeDecomposed
) {}
