package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

public record EffectModelRecord(
    String methodName,
    ActivityType.Executor executor,
    Optional<TypeMirror> returnType,
    Optional<String> durationParameter,
    Optional<String> fixedDurationExpr) {}
