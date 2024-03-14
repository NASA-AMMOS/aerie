package gov.nasa.jpl.aerie.scheduling.plan

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade
import gov.nasa.jpl.aerie.scheduling.EditablePlan
import gov.nasa.jpl.aerie.scheduling.simulation.SimulateOptions
import gov.nasa.jpl.aerie.timeline.collections.Directives
import gov.nasa.jpl.aerie.timeline.payloads.activities.Directive
import gov.nasa.jpl.aerie.timeline.plan.*
import java.sql.Connection

data class AeriePostgresEditablePlan(
    private val c: Connection,
    private val plan: AeriePostgresPlan,
    private val simFacade: SimulationFacade
): EditablePlan, Plan by plan {

  private var nextUniqueDirectiveId = run {
    val statement = c.prepareStatement("select max(id) from activity_directive where plan_id = ?;")
    statement.setInt(1, plan.id)
    getSingleLongQueryResult(statement) + 1
  }

  // A list of commits is kept without flattening them into one diff
  // to make using incremental sim easier in the future.
  private val commits = mutableListOf<Commit>()
  private var mostRecentSimInputs = object {
    val commitIndex: Int? = null
    val uncommittedChanges = listOf<Edit>()
  }

  private val uncommittedChanges = mutableListOf<Edit>()

  private val mostRecentNormalSimInfo = run {
    val statement = c.prepareStatement(
        "select max(id), plan_revision from simulation_dataset where simulation_id in (select id from simulation where plan_id = ?)"
    )
    statement.setInt(1, plan.id)
    val result = statement.executeQuery()
    if (!result.next()) null
    else {
      val simDatasetId = result.getInt(1)
      if (result.next()) throw AeriePostgresSimulationResults.DatabaseError("Expected at most one result for query, found more than one: $statement")

      val simulatedRevision = result.getInt(2);
      val planRevisionStatement = c.prepareStatement("select revision from plan where id = ?;")
      planRevisionStatement.setInt(1, plan.id)
      val currentPlanRevision = getSingleLongQueryResult(planRevisionStatement).toInt()

      object {
        val simDatasetId = simDatasetId
        val stale = currentPlanRevision == simulatedRevision
      }
    }
  }

  override fun latestResults(): SimulationResults? {
    return if (simFacade.latestDriverSimulationResults.isPresent) {
      val results = simFacade.latestDriverSimulationResults.get()
      AerieInMemorySimulationResults(
          results,
          mostRecentSimInputs.commitIndex?.let {
            it < commits.size - 1
                || mostRecentSimInputs.uncommittedChanges == uncommittedChanges
          }
              ?: true,
          plan
      )
    } else {
      mostRecentNormalSimInfo?.let {
        AeriePostgresSimulationResults(
            c,
            it.simDatasetId,
            plan,
            it.stale
        )
      }
    }
  }

  override fun create(directive: NewDirective): Long {
    val id = nextUniqueDirectiveId++
    uncommittedChanges.add(Edit.Create(directive.withId(id)))
    return id
  }

  override fun commit() {
    commits.add(Commit(rollback()))
  }

  override fun rollback() = uncommittedChanges.take(uncommittedChanges.size)

  override fun simulate(options: SimulateOptions): SimulationResults {
    TODO("oh boy")
  }

  override fun <A : Any> directives(type: String?, deserializer: (SerializedValue) -> A): Directives<A> {
    val basePlan = plan.directives(type, deserializer)

    return basePlan.unsafeOperate { opts ->
      val directives = collect(opts).toMutableList()
      for (edit in totalDiff + uncommittedChanges) {
        when (edit) {
          is Edit.Create -> if (edit.directive.type == type && edit.directive.startTime in opts.bounds) directives.add(Directive(
              deserializer(edit.directive.inner.serialize()),
              edit.directive.name,
              edit.directive.id,
              edit.directive.type,
              edit.directive.startTime,
          ))
        }
      }
      directives
    }
    // test if overloads in plan interface resolve to this correctly
  }

  val totalDiff: List<Edit>
    get() = commits.flatMap { it.diff }

}
