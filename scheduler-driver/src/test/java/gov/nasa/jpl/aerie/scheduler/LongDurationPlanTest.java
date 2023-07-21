package gov.nasa.jpl.aerie.scheduler;

import com.google.common.truth.Correspondence;
import com.google.common.truth.Truth;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.goals.ProceduralCreationGoal;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static com.google.common.truth.Truth8.assertThat;

public class LongDurationPlanTest {

  private static PrioritySolver makeProblemSolver(Problem problem) {
    return new PrioritySolver(problem);
  }

  //test mission with two primitive activity types
  private static Problem makeTestMissionAB() {
    final var banananationMissionModel = SimulationUtility.getBananaMissionModel();
    return new Problem(banananationMissionModel, h, new SimulationFacade(h, banananationMissionModel), SimulationUtility.getBananaSchedulerModel());
  }

  private final static PlanningHorizon h = new PlanningHorizon(TimeUtility.fromDOY("2025-001T01:01:01.001"), TimeUtility.fromDOY("2030-005T01:01:01.001"));
  private final static Duration t0 = h.getStartAerie();
  private final static Duration d1year = Duration.of(1, Duration.SECONDS).times(3600).times(24).times(365);
  private final static Duration d1min = Duration.of(1, Duration.MINUTES);

  private final static Duration t1year = t0.plus(d1year);
  private final static Duration t2year = t1year.plus(d1year);

  private final static Duration t3year = t2year.plus(d1year);

  private static PlanInMemory makePlanA012(Problem problem) {
    final var plan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    plan.add(SchedulingActivityDirective.of(actTypeA, t0, d1min, null, true));
    plan.add(SchedulingActivityDirective.of(actTypeA, t1year, d1min, null, true));
    plan.add(SchedulingActivityDirective.of(actTypeA, t2year, d1min, null, true));
    plan.add(SchedulingActivityDirective.of(actTypeA, t3year, d1min, null, true));
    return plan;
  }

  /** used to compare plan activities but ignore generated details like name **/
  private static boolean equalsExceptInName(SchedulingActivityDirective a, SchedulingActivityDirective b) {
    //REVIEW: maybe unify within ActivityInstance closer to data
    return Objects.equals(a.getType(), b.getType())
           && Objects.equals(a.startOffset(), b.startOffset())
           && Objects.equals(a.getEndTime(), b.getEndTime())
           && Objects.equals(a.duration(), b.duration())
           && Objects.equals(a.arguments(), b.arguments());
  }

  /** matches activities if they agree in everything except the (possibly auto-generated) names **/
  private static final Correspondence<SchedulingActivityDirective, SchedulingActivityDirective> equalExceptInName = Correspondence.from(
      LongDurationPlanTest::equalsExceptInName, "matches");

  @Test
  public void getNextSolution_initialPlanInOutput() {
    final var problem = makeTestMissionAB();
    final var expectedPlan = makePlanA012(problem);
    problem.setInitialPlan(makePlanA012(problem));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution();

    assertThat(plan).isPresent();
    Truth.assertThat(plan.get().getActivitiesByTime())
         .comparingElementsUsing(equalExceptInName)
         .containsExactlyElementsIn(expectedPlan.getActivitiesByTime());
  }

  @Test
  public void getNextSolution_proceduralGoalCreatesActivities() {
    final var problem = makeTestMissionAB();
    final var expectedPlan = makePlanA012(problem);
    final var goal = new ProceduralCreationGoal.Builder()
        .named("g0")
        .generateWith((plan) -> expectedPlan.getActivitiesByTime())
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .withinPlanHorizon(h)
        .build();
    problem.setGoals(List.of(goal));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    Truth.assertThat(plan.getActivitiesByTime())
         .comparingElementsUsing(equalExceptInName)
         .containsExactlyElementsIn(expectedPlan.getActivitiesByTime());
  }
}
