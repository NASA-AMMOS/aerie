import { expect, test } from '@playwright/test';
import req, {awaitScheduling} from '../utilities/requests.js';

test.describe('Scheduling and Decomposing Activities', () => {
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-090T00:00:00.000";
  let mission_model_id: number;
  let plan_id: number;
  let plan_revision: number;
  let specification_id: number;

  test.beforeAll(async ({request}) => {
    let rd = Math.random() * 100000;
    let jar_id = await req.uploadJarFile(request);
    // Add Mission Model
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: rd + "",
    };
    mission_model_id = await req.createMissionModel(request, model);
    await delay(1000);

    // Add Plan
    const plan_input: CreatePlanInput = {
      model_id: mission_model_id,
      name: 'test_plan' + rd,
      start_time: plan_start_timestamp,
      duration: "24:00:00"
    };
    plan_id = await req.createPlan(request, plan_input);
    plan_revision = await req.getPlanRevision(request, plan_id);

    // Add Scheduling Spec
    const schedulingSpecification: SchedulingSpecInsertInput = {
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id: plan_id,
      plan_revision: plan_revision,
      simulation_arguments: {},
      analysis_only: false
    }
    specification_id = await req.insertSchedulingSpecification(request, schedulingSpecification);

    // Add Goal
    const schedulingGoal: SchedulingGoalInsertInput =
        {
          description: "Cardinality goal",
          model_id: mission_model_id,
          name: "my second scheduling goal!" + rd,
          definition:
              `export default function cardinalityGoalExample() {
                        return Goal.CardinalityGoal({
                            activityTemplate: ActivityTemplates.parent({ label: "unlabeled"}),
                            specification: { duration: Temporal.Duration.from({ seconds: 10 }) },
                        });
                    }`
        };
    let goal_id = await req.insertSchedulingGoal(request, schedulingGoal);

    // Assign Scheduling Spec Goal
    const specGoal: SchedulingSpecGoalInsertInput = {
      // @ts-ignore
      goal_id: goal_id,
      priority: 0,
      specification_id: specification_id,
    };
    await req.createSchedulingSpecGoal(request, specGoal);
  });

  test.afterAll(async ({request}) => {
      // Deleting Plan and Model cascades the rest of the cleanup
      await req.deleteMissionModel(request, mission_model_id);
      await req.deletePlan(request, plan_id);
  });

  test('Scheduling Properly assigns Span Ids on Decomposing Activities', async ({request}) => {
    // Run Scheduling
    const schedulingResults = await awaitScheduling(request, specification_id);
    const dataset_id = schedulingResults.datasetId;

    // Check Plan Activities
    let plan = await req.getPlan(request, plan_id)
    expect(plan.id).toEqual(plan_id);
    expect(plan.activity_directives.length).toEqual(1);

    let parentActivity = plan.activity_directives.pop();
    expect(parentActivity.type).toEqual('parent');

    const simulated_activities = (await req.getSimulationDatasetByDatasetId(request, dataset_id)).simulated_activities
    expect(simulated_activities.length).toEqual(7) // 1 parent, 2 children, 4 grandchildren

    // Assert Parent Span
    let parentSpans = simulated_activities.filter(a => a.type === "parent");
    expect(parentSpans.length).toEqual(1);

    let parentSpan = parentSpans.pop();
    expect(parentSpan.parent_id).toBeNull();
    expect(parentSpan.activity_directive.id).toEqual(parentActivity.id)

    // Assert Children Spans
    let childrenSpans = simulated_activities.filter(a => a.type === "child");
    expect(childrenSpans.length).toEqual(2);
    childrenSpans.forEach(a => {
      expect(a.activity_directive).toBeNull();
      expect(a.parent_id).not.toBeNull();
      expect(a.parent_id).toEqual(parentSpan.id);
    });
    let child1 = childrenSpans.pop();
    let child2 = childrenSpans.pop();

    // Assert Grandchildren Spans
    let grandchildrenSpans = simulated_activities.filter(a => a.type === "grandchild");
    expect(grandchildrenSpans.length).toEqual(4);
    grandchildrenSpans.forEach(a => expect(a.activity_directive).toBeNull());

    let gcFirstChild = grandchildrenSpans.filter(a => a.parent_id === child1.id);
    expect(gcFirstChild.length).toEqual(2);

    let gcSecondChild = grandchildrenSpans.filter(a => a.parent_id === child2.id);
    expect(gcSecondChild.length).toEqual(2);
  });
});

function delay(ms: number) {
  return new Promise( resolve => setTimeout(resolve, ms) );
}
