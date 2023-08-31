package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

public record InputTypeRecord(
    String name,
    TypeElement declaration,
    List<ParameterRecord> parameters,
    Map<String, String> parameterUnits,
    List<ParameterValidationRecord> validations,
    MapperRecord mapper,
    ExportDefaultsStyle defaultsStyle
) {}
