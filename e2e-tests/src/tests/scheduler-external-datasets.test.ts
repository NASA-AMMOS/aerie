
import {expect, test} from "@playwright/test";
import req, {awaitScheduling, awaitSimulation} from "../utilities/requests.js";
import time from "../utilities/time.js";

/*
  This test uploads an external dataset and checks that it is possible to use an external resource in a scheduling goal
*/
test.describe.serial('Scheduling with external dataset', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T12:00:00.000";

  test('Main', async ({request}) => {
    //upload bananation jar
    const jar_id = await req.uploadJarFile(request);

    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests' + rd,
      name: 'Banananation (e2e tests)' + rd,
      version: '0.0.0' + rd,
    };
    const mission_model_id = await req.createMissionModel(request, model);
    //delay for generation
    await delay(2000);
    const plan_input: CreatePlanInput = {
      model_id: mission_model_id,
      name: 'test_plan' + rd,
      start_time: plan_start_timestamp,
      duration: time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    const plan_id = await req.createPlan(request, plan_input);

    const profile_set = {
      '/my_boolean': {
        type: 'discrete',
        schema: {
          type: 'boolean',
        },
        segments: [
          { duration: 3600000000, dynamics: false },
          { duration: 3600000000, dynamics: true },
          { duration: 3600000000, dynamics: false },
        ],
      },
    };

    const externalDatasetInput: ExternalDatasetInsertInput = {
      plan_id,
      dataset_start: plan_start_timestamp,
      profile_set,
    };

    await req.insertExternalDataset(request, externalDatasetInput);

    await awaitSimulation(request, plan_id);

    const schedulingGoal1: SchedulingGoalInsertInput =
        {
          description: "Test goal",
          model_id: mission_model_id,
          name: "ForEachGrowPeel" + rd,
          definition: `export default function myGoal() {
                  return Goal.CoexistenceGoal({
                    forEach: Discrete.Resource("/my_boolean").equal(true).assignGaps(false),
                    activityTemplate: ActivityTemplates.BiteBanana({
                      biteSize: 1,
                    }),
                    startsAt:TimingConstraint.singleton(WindowProperty.END)
                  })
                }`
        };

    const first_goal_id = await req.insertSchedulingGoal(request, schedulingGoal1);

    let plan_revision = await req.getPlanRevision(request, plan_id);

    const schedulingSpecification: SchedulingSpecInsertInput = {
      // @ts-ignore
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id: plan_id,
      plan_revision: plan_revision,
      simulation_arguments: {},
      analysis_only: false
    }
    const scheduling_specification_id = await req.insertSchedulingSpecification(request, schedulingSpecification);

    const priority = 0;
    const specGoal: SchedulingSpecGoalInsertInput = {
      goal_id: first_goal_id,
      priority: priority,
      specification_id: scheduling_specification_id,
    };
    await req.createSchedulingSpecGoal(request, specGoal);

    await awaitScheduling(request, scheduling_specification_id);
    const plan = await req.getPlan(request, plan_id)
    expect(plan.activity_directives.length).toEqual(1);
    expect(plan.activity_directives[0].startTime == "2021-001T02:00:00.000")
    await req.deletePlan(request, plan_id);
    await req.deleteMissionModel(request, mission_model_id)
  });
});

function delay(ms: number) {
  return new Promise( resolve => setTimeout(resolve, ms) );
}
