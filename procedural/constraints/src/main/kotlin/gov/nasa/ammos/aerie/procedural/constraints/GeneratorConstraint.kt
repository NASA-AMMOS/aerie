package gov.nasa.ammos.aerie.procedural.constraints

import gov.nasa.ammos.aerie.procedural.timeline.collections.Windows
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Real
import gov.nasa.ammos.aerie.procedural.timeline.ops.GeneralOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.ParallelOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.SerialConstantOps
import gov.nasa.ammos.aerie.procedural.timeline.payloads.IntervalLike
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults

/**
 * A generator-style implementation of [Constraint].
 *
 * The subclass must implement [generate], and within it call [violate] to produce violations.
 * Or if you are using Kotlin, you can use the timeline extension functions such as [windows.violateInside()][violateInside]
 * to more easily submit violations.
 */
abstract class GeneratorConstraint: Constraint {
  private val violations = mutableListOf<Violation>()

  /** Finalizes one or more violations. */
  protected fun violate(vararg v: Violation) {
    violate(v.toList())
  }

  /** Finalizes a list of violations. */
  protected fun violate(l: List<Violation>) {
    violations.addAll(l)
  }

  /** Collects a [Violations] timeline and finalizes the result. */
  protected fun violate(tl: Violations) {
    violate(tl.collect())
  }

  /** Creates a [Violations] object that violates when this profile equals a given value. */
  protected fun <V: Any> SerialConstantOps<V, *>.violateOn(v: V) = violate(Violations.on(this, v))

  /** Creates a [Violations] object that violates when this profile equals a given value. */
  protected fun Real.violateOn(n: Number) = violate(Violations.on(this, n))

  /**
   * Creates a [Violations] object that violates on every object in the timeline.
   *
   * If the object is an activity, it will record the directive or instance id.
   */
  protected fun <I: IntervalLike<I>> ParallelOps<I, *>.violateOnAll() {
    violate(Violations.onAll(this))
  }

  /** Creates a [Violations] object that violates inside each interval. */
  protected fun Windows.violateInside() = violate(Violations.inside(this))
  /** Creates a [Violations] object that violates outside each interval. */
  protected fun Windows.violateOutside() = violate(Violations.outside(this))

  /**
   * Creates a [Violations] object from two timelines, that violates whenever they have overlap.
   *
   * If either object is an activity, it will record the directive or instance id.
   */
  protected fun <V: IntervalLike<V>, W: IntervalLike<W>> GeneralOps<V, *>.mutexViolations(other: GeneralOps<W, *>) {
    violate(Violations.mutex(this, other))
  }

  /**
   * A generator function that calls [violate] to produce violations.
   */
  abstract fun generate(plan: Plan, simResults: SimulationResults)

  final override fun run(plan: Plan, simResults: SimulationResults): Violations {
    violations.clear()
    generate(plan, simResults)
    return Violations(violations)
  }
}
