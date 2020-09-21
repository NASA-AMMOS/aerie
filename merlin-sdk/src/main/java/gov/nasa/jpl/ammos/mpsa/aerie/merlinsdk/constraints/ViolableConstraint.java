package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.Objects;
import java.util.Set;

public final class ViolableConstraint implements Constraint {
  private final Constraint constraint;
  public String id;
  public String name;
  public String message;
  public String category;

  public ViolableConstraint(final Constraint constraint) {
    Objects.requireNonNull(constraint);
    this.constraint = constraint;
  }

  public ViolableConstraint withId(final String id) {
    this.id = id;
    return this;
  }

  public ViolableConstraint withName(final String name) {
    this.name = name;
    return this;
  }

  public ViolableConstraint withMessage(final String message) {
    this.message = message;
    return this;
  }

  public ViolableConstraint withCategory(final String category) {
    this.category = category;
    return this;
  }

  @Override
  public Set<String> getActivityIds() {
    return this.constraint.getActivityIds();
  }

  @Override
  public Set<String> getStateIds() {
    return this.constraint.getStateIds();
  }

  @Override
  public Set<String> getActivityTypes() {
    return this.constraint.getActivityTypes();
  }

  @Override
  public Windows getWindows() {
    return this.constraint.getWindows();
  }

  @Override
  public ConstraintStructure getStructure() {
    return this.constraint.getStructure();
  }
}
