package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import java.util.List;
import javax.lang.model.element.TypeElement;

public record InputTypeRecord(
    String name,
    TypeElement declaration,
    List<ParameterRecord> parameters,
    List<ParameterValidationRecord> validations,
    MapperRecord mapper,
    ExportDefaultsStyle defaultsStyle) {}
