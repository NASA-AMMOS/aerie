package gov.nasa.jpl.aerie.scheduler.goals

import gov.nasa.jpl.aerie.merlin.driver.*
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.procedural.scheduling.plan.Commit
import gov.nasa.jpl.aerie.procedural.scheduling.plan.EditablePlan
import gov.nasa.jpl.aerie.procedural.scheduling.plan.Edit
import gov.nasa.jpl.aerie.procedural.scheduling.plan.NewDirective
import gov.nasa.jpl.aerie.procedural.scheduling.simulation.SimulateOptions
import gov.nasa.jpl.aerie.timeline.collections.Directives
import gov.nasa.jpl.aerie.timeline.plan.Plan
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults as MerlinSimResults
import gov.nasa.jpl.aerie.timeline.plan.SimulationResults as TimelineSimResults

data class InMemoryEditablePlan(
    private val missionModel: MissionModel<*>,
    private var nextUniqueDirectiveId: Long,
    private var latestMerlinResults: MerlinSimResults?,
    private val plan: Plan
) : EditablePlan, Plan by plan {

  private var latestTimelineResults: TimelineSimResults? = null

  private val commits = mutableListOf<Commit>()
  var uncommittedChanges = mutableListOf<Edit>()
    private set

  val totalDiff: List<Edit>
    get() = commits.flatMap { it.diff }

  init {
    adaptSimulationResults()
  }

  override fun latestResults() = latestTimelineResults

  override fun create(directive: NewDirective): Long {
    class ParentSearchException(size: Int): Exception("Expected one activity with matching parent id, found $size")
    val id = nextUniqueDirectiveId++
    val parent = if (directive.start is DirectiveStart.Anchor) {
      val parentList = directives()
          .filter { it.id == (directive.start as DirectiveStart.Anchor).parentId }
          .collect(totalBounds())
      if (parentList.size != 1) throw ParentSearchException(parentList.size)
      parentList.first()
    } else null
    uncommittedChanges.add(Edit.Create(directive.resolve(id, parent)))
    return id
  }

  override fun commit() {
    commits.add(Commit(rollback()))
  }

  override fun rollback(): List<Edit> {
    val result = uncommittedChanges
    uncommittedChanges = mutableListOf()
    return result
  }

  override fun simulate(options: SimulateOptions): TimelineSimResults {
    // TODO: make configurable
    val simBounds = totalBounds()

    val allDirectives = directives().collect(simBounds)

    val schedule = mutableMapOf<ActivityDirectiveId, ActivityDirective>()
    for (directive in allDirectives) {
      val id = ActivityDirectiveId(directive.id)
      schedule[id] = ActivityDirective(
          directive.startTime,
          directive.type,
          directive.inner.arguments,
          if (directive.start is DirectiveStart.Anchor) {
            ActivityDirectiveId((directive.start as DirectiveStart.Anchor).parentId)
          } else null,
          if (directive.start is DirectiveStart.Anchor) {
            (directive.start as DirectiveStart.Anchor).anchorPoint == DirectiveStart.Anchor.AnchorPoint.Start
          } else true
      )
    }

    latestMerlinResults = SimulationDriver.simulate(
        missionModel,
        java.util.Map.of(),
        toAbsolute(simBounds.start),
        simBounds.duration(),
        toAbsolute(totalBounds().start),
        totalBounds().duration()
    ) { false }
    adaptSimulationResults()
    return latestTimelineResults!!
  }

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    val basePlan = plan.directives(type, deserializer)

    return basePlan.unsafeOperate { opts ->
      val directives = collect(opts).toMutableList()
      for (edit in totalDiff + uncommittedChanges) {
        when (edit) {
          is Edit.Create -> if (edit.directive.type == type && edit.directive.startTime in opts.bounds) directives.add(
              edit.directive.mapInner { deserializer(it.serialize()) }
          )
        }
      }
      directives
    }
    // test if overloads in plan interface resolve to this correctly
  }

  private fun adaptSimulationResults() {
    latestTimelineResults = latestMerlinResults?.let { InMemorySimulationResults(it, false, plan) }
  }
}
