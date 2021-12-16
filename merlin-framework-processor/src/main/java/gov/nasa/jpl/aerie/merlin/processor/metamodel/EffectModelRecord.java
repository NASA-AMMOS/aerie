package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;

public record EffectModelRecord(String methodName, ActivityType.Executor executor) {
}
