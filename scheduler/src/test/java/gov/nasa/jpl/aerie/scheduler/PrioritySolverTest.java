package gov.nasa.jpl.aerie.scheduler;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Correspondence;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.goals.ProceduralCreationGoal;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

public class PrioritySolverTest {
  private static PrioritySolver makeEmptyProblemSolver() {
    return new PrioritySolver(new Problem(null, h, null, null));
  }

  private static PrioritySolver makeProblemSolver(Problem problem) {
    return new PrioritySolver(problem);
  }

  @Test
  public void ctor_onEmptyProblemWorks() {
    new PrioritySolver(new Problem(null,h, null, null));
  }

  @Test
  void ctors_nullArgThrowsNPE() {
    NULL_POINTER_TESTER.testAllPublicConstructors(PrioritySolver.class);
  }

  @Test
  public void getNextSolution_onEmptyProblemGivesEmptyPlanAndOneEmptyEvaluation() {
    final var solver = makeEmptyProblemSolver();
    final var plan = solver.getNextSolution();

    assertThat(plan).isPresent();
    assertThat(plan.get().getEvaluation()).isEqualTo(new Evaluation());
    assertThat(plan.get().getActivitiesByTime()).isEmpty();
  }

  @Test
  public void getNextSolution_givesNoSolutionOnSubsequentCall() {
    final var solver = makeEmptyProblemSolver();
    solver.getNextSolution();
    final var plan1 = solver.getNextSolution();

    assertThat(plan1).isEmpty();
  }

  //test mission with two primitive activity types
  private static Problem makeTestMissionAB() {
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    return new Problem(fooMissionModel, h, new SimulationFacade(h, fooMissionModel), SimulationUtility.getFooSchedulerModel());
  }

  private final static PlanningHorizon h = new PlanningHorizon(TimeUtility.fromDOY("2025-001T01:01:01.001"), TimeUtility.fromDOY("2030-001T01:01:01.001"));
  private final static Duration t0 = h.getStartAerie();
  private final static Duration d1min = Duration.of(1, Duration.MINUTE);
  private final static Duration d1hr = Duration.of(1, Duration.HOUR);
  private final static Duration t1hr = t0.plus(d1hr);
  private final static Duration t2hr = t0.plus(d1hr.times(2));
  private final static Duration t3hr = t0.plus(d1hr.times(2));

  private static final NullPointerTester NULL_POINTER_TESTER = new NullPointerTester()
      .setDefault(Problem.class, new Problem(null, h, null, null));


  private static PlanInMemory makePlanA012(Problem problem) {
    final var plan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("ControllableDurationActivity");
    plan.add(new ActivityInstance(actTypeA, t0, d1min));
    plan.add(new ActivityInstance(actTypeA, t1hr, d1min));
    plan.add(new ActivityInstance(actTypeA, t2hr, d1min));
    return plan;
  }

  private static PlanInMemory makePlanA12(Problem problem) {
    final var plan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("ControllableDurationActivity");
    plan.add(new ActivityInstance(actTypeA, t1hr, d1min));
    plan.add(new ActivityInstance(actTypeA, t2hr, d1min));
    return plan;
  }

  private static PlanInMemory makePlanAB012(Problem problem) {
    final var plan = makePlanA012(problem);
    final var actTypeB = problem.getActivityType("OtherControllableDurationActivity");
    plan.add(new ActivityInstance(actTypeB, t0, d1min));
    plan.add(new ActivityInstance(actTypeB, t1hr, d1min));
    plan.add(new ActivityInstance(actTypeB, t2hr, d1min));
    return plan;
  }

  /** used to compare plan activities but ignore generated details like name **/
  private static boolean equalsExceptInName(ActivityInstance a, ActivityInstance b) {
    //REVIEW: maybe unify within ActivityInstance closer to data
    return Objects.equals(a.getType(), b.getType())
           && Objects.equals(a.getStartTime(), b.getStartTime())
           && Objects.equals(a.getEndTime(), b.getEndTime())
           && Objects.equals(a.getDuration(), b.getDuration())
           && Objects.equals(a.getArguments(), b.getArguments());
  }

  /** matches activities if they agree in everything except the (possibly auto-generated) names **/
  private static final Correspondence<ActivityInstance, ActivityInstance> equalExceptInName = Correspondence.from(
      PrioritySolverTest::equalsExceptInName, "matches");

  @Test
  public void getNextSolution_initialPlanInOutput() {
    final var problem = makeTestMissionAB();
    final var expectedPlan = makePlanA012(problem);
    problem.setInitialPlan(makePlanA012(problem));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution();

    assertThat(plan).isPresent();
    assertThat(plan.get().getActivitiesByTime())
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
        .build();
    problem.setGoals(List.of(goal));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    assertThat(plan.getActivitiesByTime())
        .comparingElementsUsing(equalExceptInName)
        .containsExactlyElementsIn(expectedPlan.getActivitiesByTime());
  }

  @Test
  public void getNextSolution_proceduralGoalAttachesActivitiesToEvaluation() {
    final var problem = makeTestMissionAB();
    final var expectedPlan = makePlanA012(problem);
    final var goal = new ProceduralCreationGoal.Builder()
        .named("g0")
        .generateWith((plan) -> expectedPlan.getActivitiesByTime())
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .build();
    problem.setGoals(List.of(goal));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    assertThat(plan.getEvaluation()).isNotNull();
    final var eval = plan.getEvaluation().forGoal(goal);
    assertThat(eval).isNotNull();
    assertThat(eval.getAssociatedActivities())
        .comparingElementsUsing(equalExceptInName)
        .containsExactlyElementsIn(expectedPlan.getActivitiesByTime());
  }

  @Test
  public void getNextSolution_recurrenceGoalWorks() {
    final var problem = makeTestMissionAB();
    final var goal = new RecurrenceGoal.Builder()
        .named("g0")
        .startingAt(t0)
        .endingAt(t2hr.plus(Duration.of(10, Duration.MINUTE)))
        .repeatingEvery(d1hr)
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(problem.getActivityType("ControllableDurationActivity"))
                            .duration(d1min)
                            .build())
        .build();
    problem.setGoals(List.of(goal));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    final var expectedPlan = makePlanA012(problem);
    //TODO: evaluation should have association of instances to goal
    //TODO: should ensure no other spurious acts yet need to ignore special interval activities
    //TODO: may want looser expectation (eg allow flexibility as long as right repeat pattern met)
    assertThat(equalsExceptInName(plan.getActivitiesByTime().get(0), expectedPlan.getActivitiesByTime().get(0)))
        .isTrue();
    assertThat(plan.getActivitiesByTime())
        .comparingElementsUsing(equalExceptInName)
        .containsExactlyElementsIn(expectedPlan.getActivitiesByTime()).inOrder();
  }

  @Test
  public void getNextSolution_coexistenceGoalOnActivityWorks() {
    final var problem = makeTestMissionAB();
    problem.setInitialPlan(makePlanA012(problem));
    final var actTypeA = problem.getActivityType("ControllableDurationActivity");
    final var actTypeB = problem.getActivityType("OtherControllableDurationActivity");
    final var goal = new CoexistenceGoal.Builder()
        .named("g0")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .forEach(new ActivityExpression.Builder()
                     .ofType(actTypeA)
                     .build())
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actTypeB)
                            .duration(d1min)
                            .build())
        .startsAt(TimeAnchor.START)
        .build();
    problem.setGoals(List.of(goal));
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    final var expectedPlan = makePlanAB012(problem);
    //TODO: evaluation should have association of instances to goal
    //TODO: should ensure no other spurious acts yet need to ignore special interval activities
    assertThat(plan.getActivitiesByTime())
        .comparingElementsUsing(equalExceptInName)
        .containsAtLeastElementsIn(expectedPlan.getActivitiesByTime());
  }

}
