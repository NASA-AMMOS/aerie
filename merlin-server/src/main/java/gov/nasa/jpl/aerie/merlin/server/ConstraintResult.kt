package gov.nasa.jpl.aerie.merlin.server

import gov.nasa.ammos.aerie.procedural.constraints.Violation
import gov.nasa.ammos.aerie.procedural.timeline.Interval
import java.util.*

class ConstraintResult {
  // These two will be initialized during constraint AST evaluation.
  val violations: List<Violation>

  // The rest will be initialized after AST evaluation by the constraints action.
  var constraintId: Long? = null
  var constraintRevision: Long? = null
  var constraintName: String? = null

  @JvmOverloads
  constructor(violations: List<Violation> = listOf()) {
    this.violations = violations
  }

  constructor(
    violations: List<Violation>,
    gaps: List<Interval?>,
    resourceIds: List<String>?,
    constraintId: Long?,
    constraintRevision: Long?,
    constraintName: String?
  ) {
    this.violations = violations
    this.constraintId = constraintId
    this.constraintRevision = constraintRevision
    this.constraintName = constraintName
  }

  val isEmpty: Boolean
    get() = violations.isEmpty()

  companion object {
    /**
     * Merges two results of violations and gaps into a single result.
     *
     * This function is to be called during constraint AST evaluation, before the
     * extra metadata fields are populated. All fields besides violations and gaps
     * are ignored and lost.
     */
    fun merge(l1: ConstraintResult, l2: ConstraintResult): ConstraintResult {
      val violations = ArrayList(l1.violations)
      violations.addAll(l2.violations)

      return ConstraintResult(violations)
    }
  }
}
