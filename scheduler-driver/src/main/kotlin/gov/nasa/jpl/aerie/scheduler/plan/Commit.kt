package gov.nasa.jpl.aerie.scheduler.plan

import gov.nasa.jpl.aerie.procedural.scheduling.plan.Edit

data class Commit(
    val diff: List<Edit>,
)
