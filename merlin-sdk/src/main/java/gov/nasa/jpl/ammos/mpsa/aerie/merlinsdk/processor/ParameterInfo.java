package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.type.DeclaredType;
import java.util.Collections;
import java.util.List;

final class ParameterInfo {
  private final DeclaredType type;
  private final List<Pair<String, ParameterArg>> parameterTypes;

  ParameterInfo(
      final DeclaredType type,
      final List<Pair<String, ParameterArg>> parameterTypes
  ) {
    this.type = type;
    this.parameterTypes = parameterTypes;
  }

  DeclaredType getType() {
    return type;
  }
  List<Pair<String, ParameterArg>> getParameterTypes() {
    return Collections.unmodifiableList(parameterTypes);
  }
}
