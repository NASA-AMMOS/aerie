package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import javax.lang.model.type.TypeMirror;

final class ParameterArg {
  private final TypeMirror type;
  private final boolean optional;

  ParameterArg(final TypeMirror type, final boolean optional) {
    this.type = type;
    this.optional = optional;
  }

  TypeMirror getType() {
    return this.type;
  }

  boolean isOptional() {
    return this.optional;
  }
}
