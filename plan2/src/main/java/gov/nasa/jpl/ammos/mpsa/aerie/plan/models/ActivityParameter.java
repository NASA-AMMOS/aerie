package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import java.util.Objects;

public final class ActivityParameter {
  public String type;
  public String value;

  public ActivityParameter() {}

  public ActivityParameter(final ActivityParameter other) {
    this.type = other.type;
    this.value = other.value;
  }

  @Override
  public boolean equals(final Object object) {
    if (object.getClass() != ActivityParameter.class) {
      return false;
    }

    final ActivityParameter other = (ActivityParameter)object;
    return
        (  Objects.equals(this.type, other.type)
        && Objects.equals(this.value, other.value)
        );
  }
}
