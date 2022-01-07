package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import javax.lang.model.element.TypeElement;
import java.util.List;

public sealed interface SpecificationTypeRecord permits
    ActivityTypeRecord,
    ConfigurationTypeRecord
{
    String name();
    TypeElement declaration();
    List<ParameterRecord> parameters();
    List<ParameterValidationRecord> validations();
    ActivityMapperRecord mapper();
    ActivityDefaultsStyle defaultsStyle();
}
