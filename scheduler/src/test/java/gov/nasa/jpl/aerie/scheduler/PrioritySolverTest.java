package gov.nasa.jpl.aerie.scheduler;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Correspondence;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

public class PrioritySolverTest {

  private static PrioritySolver makeEmptyProblemSolver() {
    return new PrioritySolver(new HuginnConfiguration(), new Problem(new MissionModel()));
  }

  private static PrioritySolver makeProblemSolver(Problem problem) {
    return new PrioritySolver(new HuginnConfiguration(), problem);
  }

  @Test
  public void ctor_onEmptyProblemWorks() {
    new PrioritySolver(new HuginnConfiguration(), new Problem(new MissionModel()));
  }

  private static final NullPointerTester NULL_POINTER_TESTER = new NullPointerTester()
      .setDefault(HuginnConfiguration.class, new HuginnConfiguration())
      .setDefault(Problem.class, new Problem(new MissionModel()));

  @Test
  void ctors_nullArgThrowsNPE() {
    NULL_POINTER_TESTER.testAllPublicConstructors(PrioritySolver.class);
  }

  @Test
  public void getNextSolution_onEmptyProblemGivesEmptyPlanAndOneEmptyEvaluation() {
    final var solver = makeEmptyProblemSolver();
    final var plan = solver.getNextSolution();

    assertThat(plan).isPresent();
    assertThat(plan.get().getEvaluations()).containsExactly(new Evaluation());
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
  private static MissionModel makeTestMissionAB() {
    final var mission = new MissionModel();
    final var actA = new ActivityType("A");
    mission.add(actA);
    final var actB = new ActivityType("B");
    mission.add(actB);
    return mission;
  }

  private static final HuginnConfiguration defaultConfig = new HuginnConfiguration();
  private static final Range<Time> horizon = defaultConfig.getHorizon();
  private static final Time t0 = horizon.getMinimum();
  private static final Duration d1min = Duration.ofMinutes(1.0);
  private static final Duration d1hr = Duration.ofHours(1.0);
  private static final Time t1hr = t0.plus(d1hr);
  private static final Time t2hr = t0.plus(d1hr.times(2.0));
  private static final Time t3hr = t0.plus(d1hr.times(2.0));

  private static PlanInMemory makePlanA012(Problem problem) {
    final var plan = new PlanInMemory(problem.getMissionModel());
    final var actTypeA = problem.getMissionModel().getActivityType("A");
    plan.add(new ActivityInstance("a0", actTypeA, t0, d1min));
    plan.add(new ActivityInstance("a1", actTypeA, t1hr, d1min));
    plan.add(new ActivityInstance("a2", actTypeA, t2hr, d1min));
    return plan;
  }

  private static PlanInMemory makePlanA12(Problem problem) {
    final var plan = new PlanInMemory(problem.getMissionModel());
    final var actTypeA = problem.getMissionModel().getActivityType("A");
    plan.add(new ActivityInstance("a1", actTypeA, t1hr, d1min));
    plan.add(new ActivityInstance("a2", actTypeA, t2hr, d1min));
    return plan;
  }

  private static PlanInMemory makePlanAB012(Problem problem) {
    final var plan = makePlanA012(problem);
    final var actTypeB = problem.getMissionModel().getActivityType("B");
    plan.add(new ActivityInstance("b0", actTypeB, t0, d1min));
    plan.add(new ActivityInstance("b1", actTypeB, t1hr, d1min));
    plan.add(new ActivityInstance("b2", actTypeB, t2hr, d1min));
    return plan;
  }

  /** used to compare plan activities but ignore generated details like name **/
  private static boolean equalsExceptInName(ActivityInstance a, ActivityInstance b) {
    //REVIEW: maybe unify within ActivityInstance closer to data
    return Objects.equals(a.getType(), b.getType())
           && Objects.equals(a.getStartTime(), b.getStartTime())
           && Objects.equals(a.getEndTime(), b.getEndTime())
           && Objects.equals(a.getDuration(), b.getDuration())
           && Objects.equals(a.getParameters(), b.getParameters());
  }

  /** matches activities if they agree in everything except the (possibly auto-generated) names **/
  private static final Correspondence<ActivityInstance, ActivityInstance> equalExceptInName = Correspondence.from(
      PrioritySolverTest::equalsExceptInName, "matches");

  @Test
  public void getNextSolution_initialPlanInOutput() {
    final var problem = new Problem(makeTestMissionAB());
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
    final var problem = new Problem(makeTestMissionAB());
    final var expectedPlan = makePlanA012(problem);
    final var goal = new ProceduralCreationGoal.Builder()
        .named("g0")
        .generateWith((plan) -> expectedPlan.getActivitiesByTime())
        .forAllTimeIn(horizon)
        .build();
    problem.add(goal);
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    assertThat(plan.getActivitiesByTime())
        .comparingElementsUsing(equalExceptInName)
        .containsExactlyElementsIn(expectedPlan.getActivitiesByTime());
  }

  @Test
  public void getNextSolution_proceduralGoalAttachesActivitiesToEvaluation() {
    final var problem = new Problem(makeTestMissionAB());
    final var expectedPlan = makePlanA012(problem);
    final var goal = new ProceduralCreationGoal.Builder()
        .named("g0")
        .generateWith((plan) -> expectedPlan.getActivitiesByTime())
        .forAllTimeIn(horizon)
        .build();
    problem.add(goal);
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    assertThat(plan.getEvaluations()).hasSize(1);
    final var eval = plan.getEvaluations().stream().findFirst().orElseThrow().forGoal(goal);
    assertThat(eval).isNotNull();
    assertThat(eval.getAssociatedActivities())
        .comparingElementsUsing(equalExceptInName)
        .containsExactlyElementsIn(expectedPlan.getActivitiesByTime());
  }

  @Test
  public void getNextSolution_recurrenceGoalWorks() {
    //hack to reset horizon pending AMaillard fix to remove setHorizon static semantics
    TimeWindows.setHorizon(horizon.getMinimum(), horizon.getMaximum());

    final var problem = new Problem(makeTestMissionAB());
    final var goal = new RecurrenceGoal.Builder()
        .named("g0")
        .startingAt(t0).endingAt(t2hr.plus(Duration.ofMinutes(10)))
        .repeatingEvery(d1hr)
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(problem.getMissionModel().getActivityType("A"))
                            .duration(d1min)
                            .build())
        .build();
    problem.add(goal);
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    final var expectedPlan = makePlanA12(problem);
    //TODO: evaluation should have association of instances to goal
    //TODO: should ensure no other spurious acts yet need to ignore special window activities
    //TODO: may want looser expectation (eg allow flexibility as long as right repeat pattern met)
    assertThat(equalsExceptInName(plan.getActivitiesByTime().get(0), expectedPlan.getActivitiesByTime().get(0)))
        .isTrue();
    assertThat(plan.getActivitiesByTime())
        .comparingElementsUsing(equalExceptInName)
        .containsExactlyElementsIn(expectedPlan.getActivitiesByTime()).inOrder();
  }

  @Test
  public void getNextSolution_coexistenceGoalOnActivityWorks() {
    //hack to reset horizon pending AMaillard fix to remove setHorizon static semantics
    TimeWindows.setHorizon(horizon.getMinimum(), horizon.getMaximum());

    final var problem = new Problem(makeTestMissionAB());
    problem.setInitialPlan(makePlanA012(problem));
    final var actTypeA = problem.getMissionModel().getActivityType("A");
    final var actTypeB = problem.getMissionModel().getActivityType("B");
    final var goal = new CoexistenceGoal.Builder()
        .named("g0")
        .forAllTimeIn(horizon)
        .forEach(new ActivityExpression.Builder()
                     .ofType(actTypeA)
                     .build())
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actTypeB)
                            .duration(d1min)
                            .build())
        .startsAt(TimeAnchor.START)
        .build();
    problem.add(goal);
    final var solver = makeProblemSolver(problem);

    final var plan = solver.getNextSolution().orElseThrow();

    final var expectedPlan = makePlanAB012(problem);
    //TODO: evaluation should have association of instances to goal
    //TODO: should ensure no other spurious acts yet need to ignore special window activities
    assertThat(plan.getActivitiesByTime())
        .comparingElementsUsing(equalExceptInName)
        .containsAtLeastElementsIn(expectedPlan.getActivitiesByTime());
  }

}
