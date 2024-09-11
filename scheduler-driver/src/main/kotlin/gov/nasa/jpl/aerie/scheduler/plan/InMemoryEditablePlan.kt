package gov.nasa.jpl.aerie.scheduler.plan

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.ammos.aerie.procedural.scheduling.plan.Edit
import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan
import gov.nasa.ammos.aerie.procedural.scheduling.plan.NewDirective
import gov.nasa.ammos.aerie.procedural.scheduling.simulation.SimulateOptions
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyDirective
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Directive
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.DirectiveStart
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType
import gov.nasa.jpl.aerie.scheduler.DirectiveIdGenerator
import gov.nasa.jpl.aerie.scheduler.model.*
import gov.nasa.jpl.aerie.types.ActivityDirectiveId
import kotlin.jvm.optionals.getOrNull
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults as TimelineSimResults

data class InMemoryEditablePlan(
    private var idGenerator: DirectiveIdGenerator,
    private val plan: SchedulerToProcedurePlanAdapter,
    private val simulationFacade: SimulationFacade,
    private val lookupActivityType: (String) -> ActivityType
) : EditablePlan, Plan by plan {

  private val commits = mutableListOf<Commit>()
  var uncommittedChanges = mutableListOf<Edit>()
    private set

  val totalDiff: List<Edit>
    get() = commits.flatMap { it.diff }

  override fun latestResults() =
    simulationFacade.latestSimulationData.getOrNull()
      ?.let { MerlinToProcedureSimulationResultsAdapter(it.driverResults, false, plan) }

  override fun create(directive: NewDirective): ActivityDirectiveId {
    class ParentSearchException(id: ActivityDirectiveId, size: Int): Exception("Expected one parent activity with id $id, found $size")
    val id = idGenerator.next()
    val parent = when (val s = directive.start) {
      is DirectiveStart.Anchor -> {
        val parentList = directives()
            .filter { it.id == s.parentId }
            .collect(totalBounds())
        if (parentList.size != 1) throw ParentSearchException(s.parentId, parentList.size)
        parentList.first()
      }
      is DirectiveStart.Absolute -> null
    }
    val resolved = directive.resolve(id, parent)
    uncommittedChanges.add(Edit.Create(resolved))
    resolved.validateArguments(lookupActivityType)
    plan.add(resolved.toSchedulingActivity(lookupActivityType, true))
    return id
  }

  override fun commit() {
    val committedEdits = uncommittedChanges
    uncommittedChanges = mutableListOf()
    commits.add(Commit(committedEdits))
  }

  override fun rollback(): List<Edit> {
    val result = uncommittedChanges
    uncommittedChanges = mutableListOf()
    for (edit in result) {
      when (edit) {
        is Edit.Create -> {
          plan.remove(edit.directive.toSchedulingActivity(lookupActivityType, true))
        }
      }
    }
    return result
  }

  override fun simulate(options: SimulateOptions): TimelineSimResults {
    simulationFacade.simulateWithResults(plan, options.pause.resolve(this))
    return latestResults()!!
  }

  companion object {
    fun Directive<AnyDirective>.validateArguments(lookupActivityType: (String) -> ActivityType) {
      lookupActivityType(type).specType.inputType.validateArguments(inner.arguments)
    }

    @JvmStatic fun Directive<AnyDirective>.toSchedulingActivity(lookupActivityType: (String) -> ActivityType, isNew: Boolean) = SchedulingActivity(
        id,
        lookupActivityType(type),
        when (val s = start) {
          is DirectiveStart.Absolute -> s.time
          is DirectiveStart.Anchor -> s.offset
        },
        when (val d = lookupActivityType(type).durationType) {
          is DurationType.Controllable -> {
            inner.arguments[d.parameterName]?.asInt()?.let { Duration(it.get()) }
          }
          is DurationType.Parametric -> {
            d.durationFunction.apply(inner.arguments)
          }
          is DurationType.Fixed -> {
            d.duration
          }
          else -> Duration.ZERO
        },
        inner.arguments,
        null,
        when (val s = start) {
          is DirectiveStart.Absolute -> null
          is DirectiveStart.Anchor -> s.parentId
        },
      when (val s = start) {
        is DirectiveStart.Absolute -> true
        is DirectiveStart.Anchor -> s.anchorPoint == DirectiveStart.Anchor.AnchorPoint.Start
      },
      isNew
    )
  }
}
