package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConstraintResult {
  // These two will be initialized during constraint AST evaluation.
  public final List<Violation> violations;
  public final List<Interval> gaps;

  // The rest will be initialized after AST evaluation by the constraints action.
  public ConstraintType constraintType;
  public List<String> resourceIds;
  public Long constraintId;
  public String constraintName;

  public ConstraintResult() {
    this(List.of(), List.of());
  }

  public ConstraintResult(List<Violation> violations, List<Interval> gaps) {
    this.violations = violations;
    this.gaps = gaps;
  }

  public ConstraintResult(
      final List<Violation> violations,
      final List<Interval> gaps,
      final ConstraintType constraintType,
      final List<String> resourceIds,
      final Long constraintId,
      final String constraintName
  ) {
    this.violations = violations;
    this.gaps = gaps;
    this.constraintType = constraintType;
    this.resourceIds = resourceIds;
    this.constraintId = constraintId;
    this.constraintName = constraintName;
  }

  public boolean isEmpty() {
    return violations.isEmpty() && gaps.isEmpty();
  }

  /**
   * Merges two results of violations and gaps into a single result.
   *
   * This function is to be called during constraint AST evaluation, before the
   * extra metadata fields are populated. All fields besides violations and gaps
   * are ignored and lost.
   */
  public static ConstraintResult merge(ConstraintResult l1, ConstraintResult l2) {
    final var violations = new ArrayList<>(l1.violations);
    violations.addAll(l2.violations);

    final var gaps = new ArrayList<>(l1.gaps);
    gaps.addAll(l2.gaps);

    return new ConstraintResult(violations, gaps);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConstraintResult that = (ConstraintResult) o;
    return violations.equals(that.violations)
           && gaps.equals(that.gaps)
           && constraintType == that.constraintType
           && Objects.equals(resourceIds, that.resourceIds)
           && Objects.equals(constraintId, that.constraintId)
           && Objects.equals(constraintName, that.constraintName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(violations, gaps, constraintType, resourceIds, constraintId, constraintName);
  }
}
