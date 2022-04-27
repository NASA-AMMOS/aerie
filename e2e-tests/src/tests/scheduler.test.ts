import { expect, test } from '@playwright/test';
import req from '../utilities/requests';
import time from '../utilities/time';

test.describe('Scheduling', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T12:00:00.000";
  let jar_id: number;
  let mission_model_id: number;
  let plan_id: number;
  let goal_id: number;
  let plan_revision: number;
  let specification_id: number;

  test('Upload jar and create mission model', async ({ request }) => {
    //upload bananation jar
    jar_id = await req.uploadJarFile(request);

    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests' + rd,
      name: 'Banananation (e2e tests)'+rd,
      version: '0.0.0'+ rd,
    };
    mission_model_id = await req.createMissionModel(request, model);
    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual("number");
    //delay for generation
    await delay(2000);
  });

  test('Create Plan', async ({ request }) => {
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input);
    expect(plan_id).not.toBeNull();
    expect(plan_id).toBeDefined();
    expect(typeof plan_id).toEqual("number");
  });

  test('Create Simulation', async ({ request }) => {
    const simulation : SimulationCreation = {
      plan_id: plan_id,
      arguments : {},
    };
    const simulation_id = await req.createSimulation(request, simulation);
    expect(simulation_id).not.toBeNull();
    expect(simulation_id).toBeDefined();
    expect(typeof simulation_id).toEqual("number");
  });

  test('Create Scheduling goal', async ({ request }) =>{

    const schedulingGoal : SchedulingGoalInsertInput =
        {
          last_modified_by : "test",
          description: "Test goal",
          author:"Test",
          model_id: mission_model_id,
          name: "my first scheduling goal!"+rd,
          definition: `export default function myGoal() {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate: ActivityTemplates.PeelBanana({
                      peelDirection: 'fromStem',
                    }),
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }`
        };

    goal_id = await req.insertSchedulingGoal(request, schedulingGoal);
    expect(goal_id).not.toBeNull();
    expect(goal_id).toBeDefined();
    expect(typeof goal_id).toEqual("number");
  });

  test('Get Plan Revision', async ({ request }) => {
    plan_revision = await req.getPlanRevision(request, plan_id);
    expect(plan_revision).not.toBeNull();
    expect(plan_revision).toBeDefined();
    expect(typeof plan_revision).toEqual("number");
  });

  test('Create Scheduling Specification', async ({ request }) => {
    const schedulingSpecification : SchedulingSpecInsertInput = {
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id : plan_id,
      plan_revision : plan_revision,
      simulation_arguments : {}
    }
    specification_id = await req.insertSchedulingSpecification(request, schedulingSpecification);
    expect(specification_id).not.toBeNull();
    expect(specification_id).toBeDefined();
    expect(typeof specification_id).toEqual("number");
  });

  test('Create Scheduling Specification Goal', async ({ request }) => {
    const priority = 0;
    const specGoal: SchedulingSpecGoalInsertInput = {
      // @ts-ignore
      goal_id: goal_id,
      priority: priority,
      specification_id: specification_id,
    };
    const returned_spec_id = await req.createSchedulingSpecGoal(request, specGoal);
    expect(returned_spec_id).not.toBeNull();
    expect(returned_spec_id).toBeDefined();
    expect(returned_spec_id).toEqual(specification_id);
  });

  test('Run scheduling', async ({ request }) => {
    let status_local: string;
    const { reason, status } = await req.schedule(request, specification_id);
    expect(status).not.toBeNull();
    expect(status).toBeDefined();
    const max_it = 10;
    let it = 0;
    while (it++ <  max_it && status == "incomplete"){
      const { reason, status } = await req.schedule(request, specification_id);
      status_local = status;
      expect(status).not.toBeNull();
      expect(status).toBeDefined();
      await delay(1000);
    }
    expect(status_local).toEqual("complete")
  });

  test('Get Plan', async ({ request }) => {
    //check number of activities
    const plan = await req.getPlan(request, plan_id)
    expect(plan).not.toBeNull();
    expect(plan).toBeDefined();
    expect(plan.id).toEqual(plan_id);
    expect(plan.activities.length).toEqual(12);
  });

  test('Delete plan', async ({ request }) => {
    //delete plan
    const deleted_plan_id = await req.deletePlan(request, plan_id);
    expect(deleted_plan_id).not.toBeNull();
    expect(deleted_plan_id).toBeDefined();
    expect(deleted_plan_id).toEqual(plan_id);
  });

  test('Delete mission model', async ({ request }) => {
    //delete mission model
    const deleted_mission_model_id = await req.deleteMissionModel(request, mission_model_id)
    expect(deleted_mission_model_id).not.toBeNull();
    expect(deleted_mission_model_id).toBeDefined();
    expect(deleted_mission_model_id).toEqual(mission_model_id);
  });

  });

function delay(ms: number) {
  return new Promise( resolve => setTimeout(resolve, ms) );
}
