
import {expect, test} from "@playwright/test";
import req, {awaitScheduling, awaitSimulation} from "../utilities/requests.js";
import time from "../utilities/time.js";

/*
This is testing the check on Plan Revision before loading initial simulation results. We inject results associated with an old plan revision.
In these results, there is only one GrowBanana activity instead of the actual 2 present in the latest plan revision.
A coexistence goal attaching to GrowBanana activities shows that the scheduler did not use the stale sim results.
*/
test.describe.serial('Scheduling with initial sim results', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T12:00:00.000";

  test('Main', async ({ request }) => {
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
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    const plan_id = await req.createPlan(request, plan_input);

    const firstGrowBananaToInsert : ActivityInsertInput =
        {
          //no arguments to ensure that the scheduler is getting effective arguments
          arguments : {},
          plan_id: plan_id,
          type : "GrowBanana",
          start_offset : "1h"
        };

    await req.insertActivity(request, firstGrowBananaToInsert);

    await awaitSimulation(request, plan_id);

    const secondGrowBananaToInsert : ActivityInsertInput =
        {
          //no arguments to ensure that the scheduler is getting effective arguments
          arguments : {},
          plan_id: plan_id,
          type : "GrowBanana",
          start_offset : "2h"
        };

    await req.insertActivity(request, secondGrowBananaToInsert);

    const schedulingGoal1 : SchedulingGoalInsertInput =
        {
          description: "Test goal",
          model_id: mission_model_id,
          name: "ForEachGrowPeel"+rd,
          definition: `export default function myGoal() {
                  return Goal.CoexistenceGoal({
                    forEach: ActivityExpression.ofType(ActivityType.GrowBanana),
                    activityTemplate: ActivityTemplates.BiteBanana({
                      biteSize: 1,
                    }),
                    startsAt:TimingConstraint.singleton(WindowProperty.END)
                  })
                }`
        };

    const first_goal_id = await req.insertSchedulingGoal(request, schedulingGoal1);

    let plan_revision = await req.getPlanRevision(request, plan_id);

    const schedulingSpecification : SchedulingSpecInsertInput = {
      // @ts-ignore
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id : plan_id,
      plan_revision : plan_revision,
      simulation_arguments : {},
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

    const { status, datasetId } = await awaitScheduling(request, scheduling_specification_id);

    expect(status).toEqual("complete")
    expect(datasetId).not.toBeNull();

    const plan = await req.getPlan(request, plan_id)
    expect(plan.activity_directives.length).toEqual(4);

    //delete plan
    const deleted_plan_id = await req.deletePlan(request, plan_id);
    expect(deleted_plan_id).not.toBeNull();
    expect(deleted_plan_id).toBeDefined();
    expect(deleted_plan_id).toEqual(plan_id);

    //delete mission model
    const deleted_mission_model_id = await req.deleteMissionModel(request, mission_model_id)
    expect(deleted_mission_model_id).not.toBeNull();
    expect(deleted_mission_model_id).toBeDefined();
    expect(deleted_mission_model_id).toEqual(mission_model_id);
  });


  /* In this test, we load simulation results with the current plan revision but with a different sim config. If the
   injected results are picked up, the goal will not be satisfied and the number of activities will stay at its original value.
   */
  test('Scheduling sim results 2', async ({ request }) => {
    const rd = Math.random() * 100;
    const plan_start_timestamp = "2021-001T00:00:00.000";
    const plan_end_timestamp = "2021-001T12:00:00.000";

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
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    const plan_id = await req.createPlan(request, plan_input);

    const simulation_id = await req.getSimulationId(request, plan_id);

    const simulation_template : InsertSimulationTemplateInput = {
      model_id: mission_model_id,
      arguments: {
        "initialPlantCount": 400,
      },
      description: 'Template for Plan ' +plan_id
    };

    await req.insertAndAssociateSimulationTemplate(request, simulation_template, simulation_id);

    await awaitSimulation(request, plan_id);

    const empty_simulation_template : InsertSimulationTemplateInput = {
      model_id: mission_model_id,
      arguments: {
      },
      description: 'Template for Plan ' +plan_id
    };

    await req.insertAndAssociateSimulationTemplate(request, empty_simulation_template, simulation_id);

    const schedulingGoal1 : SchedulingGoalInsertInput =
        {
          description: "Test goal",
          model_id: mission_model_id,
          name: "ForEachPlanLessThan300"+rd,
          definition: `export default () => Goal.CoexistenceGoal({
            forEach: Real.Resource("/plant").lessThan(300),
            activityTemplate: ActivityTemplates.GrowBanana({quantity: 10, growingDuration: Temporal.Duration.from({minutes:1}) }),
            startsAt: TimingConstraint.singleton(WindowProperty.START)
          })`
        };

    const first_goal_id = await req.insertSchedulingGoal(request, schedulingGoal1);

    const plan_revision = await req.getPlanRevision(request, plan_id);

    const schedulingSpecification : SchedulingSpecInsertInput = {
      // @ts-ignore
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id : plan_id,
      plan_revision : plan_revision,
      simulation_arguments : {},
      analysis_only: false
    }
    const specification_id = await req.insertSchedulingSpecification(request, schedulingSpecification);

    const priority = 0;
    const specGoal: SchedulingSpecGoalInsertInput = {
      goal_id: first_goal_id,
      priority: priority,
      specification_id: specification_id,
    };
    await req.createSchedulingSpecGoal(request, specGoal);

    await awaitScheduling(request, specification_id);

    const plan = await req.getPlan(request, plan_id)
    expect(plan.activity_directives.length).toEqual(1);

    //delete plan
    const deleted_plan_id = await req.deletePlan(request, plan_id);
    expect(deleted_plan_id).not.toBeNull();
    expect(deleted_plan_id).toBeDefined();
    expect(deleted_plan_id).toEqual(plan_id);

    //delete mission model
    const deleted_mission_model_id = await req.deleteMissionModel(request, mission_model_id)
    expect(deleted_mission_model_id).not.toBeNull();
    expect(deleted_mission_model_id).toBeDefined();
    expect(deleted_mission_model_id).toEqual(mission_model_id);
  });

  /* In this test, we inject simulation results to test that the scheduler is loading them properly. If they are picked up,
  there should be no activity in the plan (plant>300). Otherwise, there should be one activity.
   */
  test('Scheduling sim results 3', async ({ request }) => {
    const rd = Math.random() * 100;
    const plan_start_timestamp = "2021-001T00:00:00.000";
    const plan_end_timestamp = "2021-001T12:00:00.000";
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
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    const plan_id = await req.createPlan(request, plan_input);

    const simId = await req.getSimulationId(request, plan_id);
    let plan_revision = await req.getPlanRevision(request, plan_id);

    const datasetId = await req.insertSimulationDataset(
        request,
        simId,
        plan_start_timestamp,
        plan_end_timestamp,
        "success",
        {},
        plan_revision);

    const profileId = await req.insertProfile(request, datasetId, "12h", "/plant", { "type": "discrete", "schema": { "type": "int" } });
    await req.insertProfileSegment(request, datasetId, 400, false, profileId, "0h");

    const schedulingGoal1 : SchedulingGoalInsertInput =
        {
          description: "Test goal",
          model_id: mission_model_id,
          name: "ForEachPlanLessThan300"+rd,
          definition: `export default () => Goal.CoexistenceGoal({
            forEach: Real.Resource("/plant").lessThan(300),
            activityTemplate: ActivityTemplates.GrowBanana({quantity: 10, growingDuration: Temporal.Duration.from({minutes:1}) }),
            startsAt: TimingConstraint.singleton(WindowProperty.START)
          })`
        };

    const first_goal_id = await req.insertSchedulingGoal(request, schedulingGoal1);

    plan_revision = await req.getPlanRevision(request, plan_id);

    const schedulingSpecification : SchedulingSpecInsertInput = {
      // @ts-ignore
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id : plan_id,
      plan_revision : plan_revision,
      simulation_arguments : {},
      analysis_only: false
    }
    const specification_id = await req.insertSchedulingSpecification(request, schedulingSpecification);

    const priority = 0;
    const specGoal: SchedulingSpecGoalInsertInput = {
      goal_id: first_goal_id,
      priority: priority,
      specification_id: specification_id,
    };
    await req.createSchedulingSpecGoal(request, specGoal);

    await awaitScheduling(request, specification_id);

    const plan = await req.getPlan(request, plan_id)
    expect(plan.activity_directives.length).toEqual(0);

    //delete plan
    await req.deletePlan(request, plan_id);

    //delete mission model
    await req.deleteMissionModel(request, mission_model_id)
  });
});


function delay(ms: number) {
  return new Promise( resolve => setTimeout(resolve, ms) );
}
