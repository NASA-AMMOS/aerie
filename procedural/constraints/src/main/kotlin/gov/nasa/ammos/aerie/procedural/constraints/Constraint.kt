package gov.nasa.ammos.aerie.procedural.constraints

import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults

/** The interface that all constraints must satisfy. */
interface Constraint {
  /**
   * Run the constraint on a plan.
   *
   * The provided collect options are the options that the [Violations] result will be collected on after
   * the constraint is run. The constraint does not need to use the options unless it collects a timeline prematurely.
   *
   * @param plan the plan to check the constraint on
   * @param simResults the [SimulationResults] that the result will be collected with
   */
  fun run(plan: Plan, simResults: SimulationResults): Violations

  /**
   * Default violation message to be displayed to user.
   *
   * Can be overridden on a violation-by-violation basis by manually specifying
   * it in the [Violation] object.
   */
  fun message(): String? = null
}
