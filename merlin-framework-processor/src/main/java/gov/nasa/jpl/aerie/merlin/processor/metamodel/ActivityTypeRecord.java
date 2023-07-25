package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ActivityTypeRecord(
    String fullyQualifiedClass,
    String name,
    InputTypeRecord inputType,
    Optional<EffectModelRecord> effectModel,
    String description,
    Map<String, String> parameterDescriptions,
    Map<String, String> units
) {
  public TypeName getOutputTypeName() {
    return this.effectModel
        .flatMap(EffectModelRecord::returnType)
        .map(TypeName::get)
        .map(TypeName::box)
        .orElseGet(() -> TypeName.get(Unit.class));
  }
}
