package gov.nasa.jpl.aerie.scheduling

import gov.nasa.jpl.aerie.timeline.CollectOptions

interface Procedure {
  fun run(plan: EditablePlan, options: CollectOptions)
}
