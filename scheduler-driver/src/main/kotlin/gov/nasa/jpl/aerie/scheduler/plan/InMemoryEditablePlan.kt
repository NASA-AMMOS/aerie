package gov.nasa.jpl.aerie.scheduler.plan

import gov.nasa.jpl.aerie.merlin.driver.MissionModel
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.ammos.aerie.procedural.scheduling.plan.Edit
import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan
import gov.nasa.ammos.aerie.procedural.scheduling.plan.NewDirective
import gov.nasa.ammos.aerie.procedural.scheduling.simulation.SimulateOptions
import gov.nasa.jpl.aerie.scheduler.model.ActivityType
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade
import gov.nasa.ammos.aerie.timeline.collections.Directives
import gov.nasa.ammos.aerie.timeline.payloads.activities.AnyDirective
import gov.nasa.ammos.aerie.timeline.payloads.activities.Directive
import gov.nasa.ammos.aerie.timeline.payloads.activities.DirectiveStart
import gov.nasa.ammos.aerie.timeline.plan.Plan
import java.time.Instant
import kotlin.jvm.optionals.getOrNull
import kotlin.math.absoluteValue
import gov.nasa.ammos.aerie.timeline.plan.SimulationResults as TimelineSimResults

data class InMemoryEditablePlan(
    private val missionModel: MissionModel<*>,
    private var nextUniqueDirectiveId: Long,
    private val plan: Plan,
    private val simulationFacade: SimulationFacade,
    private val lookupActivityType: (String) -> ActivityType
) : EditablePlan, Plan {

  private val commits = mutableListOf<Commit>()
  var uncommittedChanges = mutableListOf<Edit>()
    private set

  val totalDiff: List<Edit>
    get() = commits.flatMap { it.diff }

  override fun latestResults() = simulationFacade.latestDriverSimulationResults.getOrNull()?.let { MerlinToProcedureSimulationResultsAdapter(it, false, plan, simulationFacade.activityIdCorrespondence) }

  override fun create(directive: NewDirective): Long {
    class ParentSearchException(id: Long, size: Int): Exception("Expected one parent activity with id $id, found $size")
    val id = nextUniqueDirectiveId++
    val parent = when (val s = directive.start) {
      is DirectiveStart.Anchor -> {
        val parentList = directives()
            .filter { it.id.absoluteValue == s.parentId.absoluteValue }
            .collect(totalBounds())
        if (parentList.size != 1) throw ParentSearchException(s.parentId, parentList.size)
        parentList.first()
      }
      is DirectiveStart.Absolute -> null
    }
    val resolved = directive.resolve(id, parent)
    uncommittedChanges.add(Edit.Create(resolved))
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
    val plan = totalDiff.map {
      when (it) {
        is Edit.Create -> it.directive.toSchedulingActivityDirective(lookupActivityType)
      }
    } + plan.directives().collect().map { it.toSchedulingActivityDirective(lookupActivityType) }
    simulationFacade.removeAndInsertActivitiesFromSimulation(plan, plan)
    simulationFacade.computeSimulationResultsUntil(options.pause.resolve(this))
    return latestResults()!!
  }

  // These cannot be implemented with the by keyword,
  // because directives() below needs a custom implementation.
  override fun totalBounds() = plan.totalBounds()
  override fun toRelative(abs: Instant) = plan.toRelative(abs)
  override fun toAbsolute(rel: Duration) = plan.toAbsolute(rel)

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    val basePlan = plan.directives(type, deserializer)

    return basePlan.unsafeOperate { opts ->
      val directives = collect(opts).toMutableList()
      for (edit in totalDiff + uncommittedChanges) {
        when (edit) {
          is Edit.Create -> if (type == null || edit.directive.type == type && edit.directive.startTime in opts.bounds) directives.add(
              edit.directive.mapInner { deserializer(it.serialize()) }
          )
        }
      }
      directives
    }
  }

  companion object {
    @JvmStatic fun Directive<AnyDirective>.toSchedulingActivityDirective(lookupActivityType: (String) -> ActivityType) = SchedulingActivityDirective(
        SchedulingActivityDirectiveId(id),
        lookupActivityType(type),
        when (val s = start) {
          is DirectiveStart.Absolute -> s.time
          is DirectiveStart.Anchor -> s.offset
        },
        Duration.ZERO,
        inner.arguments,
        null,
        when (val s = start) {
          is DirectiveStart.Absolute -> null
          is DirectiveStart.Anchor -> SchedulingActivityDirectiveId(s.parentId)
        },
        start is DirectiveStart.Anchor && (start as DirectiveStart.Anchor).anchorPoint == DirectiveStart.Anchor.AnchorPoint.Start
    )
  }
}
