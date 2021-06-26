package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.Objects;

public final class ParameterSchema {
  public final String name;
  public final ValueSchema schema;

  public ParameterSchema(final String name, final ValueSchema schema) {
    this.name = name;
    this.schema = schema;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof ParameterSchema)) return false;
    final var other = (ParameterSchema) obj;

    return Objects.equals(this.name, other.name) && Objects.equals(this.schema, other.schema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.schema);
  }

  @Override
  public String toString() {
    return this.name + ": " + this.schema;
  }
}
