package gov.nasa.jpl.aerie.scheduling

import gov.nasa.jpl.aerie.scheduling.plan.Edit
import gov.nasa.jpl.aerie.scheduling.plan.NewDirective
import gov.nasa.jpl.aerie.scheduling.simulation.SimulateOptions
import gov.nasa.jpl.aerie.timeline.plan.Plan
import gov.nasa.jpl.aerie.timeline.plan.SimulationResults

interface EditablePlan: Plan {
  fun latestResults(): SimulationResults?

  fun create(directive: NewDirective): Long
  fun commit()
  fun rollback(): List<Edit>

  fun simulate(options: SimulateOptions = SimulateOptions()): SimulationResults
}
