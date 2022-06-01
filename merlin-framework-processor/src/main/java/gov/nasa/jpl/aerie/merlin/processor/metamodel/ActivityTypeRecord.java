package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

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
    Optional<EffectModelRecord> effectModel
  ) implements ExportTypeRecord {
  public TypeName getOutputTypeName() {
    return this.effectModel
        .flatMap(EffectModelRecord::returnType)
        .map(TypeName::get)
        .map(TypeName::box)
        .orElseGet(() -> TypeName.get(Unit.class));
  }
}
