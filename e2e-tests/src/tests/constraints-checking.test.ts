import { expect, test } from '@playwright/test';
import req from '../utilities/requests';
import time from '../utilities/time';

test.describe('Constraints', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T12:00:00.000";
  let jar_id: number;
  let mission_model_id: number;
  let plan_id: number;

  test('Upload jar and create mission model', async ({request}) => {
    //upload bananation jar
    jar_id = await req.uploadJarFile(request);

    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests' + rd,
      name: 'Banananation (e2e tests)' + rd,
      version: '0.0.0' + rd,
    };
    mission_model_id = await req.createMissionModel(request, model);
    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual("number");
    //delay for generation
    await delay(2000);
  });

  test('Create Plan', async ({request}) => {
    const plan_input: CreatePlanInput = {
      model_id: mission_model_id,
      name: 'test_plan' + rd,
      start_time: plan_start_timestamp,
      duration: time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input);
    expect(plan_id).not.toBeNull();
    expect(plan_id).toBeDefined();
    expect(typeof plan_id).toEqual("number");
  });

  test('Add Activities', async  ({request}) => {
    console.log(plan_id);
    const activities: ActivityInsertInput[] = [
        {
          arguments: {
            temperature: 360.0,
            tbSugar: 2,
            glutenFree: true
          },
          plan_id: plan_id,
          start_offset: "02:00:00.000",
          type: "BakeBananaBread"
        },
        {
          arguments: {
            temperature: 375.0,
            tbSugar: 3,
            glutenFree: false
          },
          plan_id: plan_id,
          start_offset: "04:00:00.000",
          type: "BakeBananaBread"
        },
    ];
    for (const activity of activities) {
      const activity_id = await req.addActivity(request, activity);
      expect(activity_id).not.toBeNull();
      expect(activity_id).toBeDefined();
      expect(typeof activity_id).toEqual("number");
    }
  });

  //TODO: add external profile
  test('Add External Profile', async  ({request}) => {
    const profiles: ExternalDataset[] = [
      {
        plan_id: plan_id,
        datasetStart: "10:00:00.000",
        profileSet: `{
          externalProfile1: {
            type: "real",
            segments: [
              {
                duration: 43200000000,
                dynamics: {
                  initial: 500,
                  rate: -0.555555
                }
              }
            ]
          }
        }`
      },
      {
        plan_id: plan_id,
        datasetStart: "8:00:00.000",
        profileSet: `{
          externalProfile2: {
            type: "discrete",
            schema: {
              type: "boolean"
            },
            segments: [
              {
                duration: 30000000,
                dynamics: true
              },
              {
                duration: 30000000,
                dynamics: false
              }
            ]
          }
        }`
      }
    ];
    //TODO: Fix.
    //for (const profile of profiles) {
      const dataset_id = await req.addExternalProfile(request, profiles[0]);
      expect(dataset_id).not.toBeNull();
      expect(dataset_id).toBeDefined();
      expect(typeof dataset_id).toEqual("number");
    //}
  });

  test('Create Simulation', async ({request}) => {
    const simulation: SimulationCreation = {
      plan_id: plan_id,
      arguments: {},
    };
    const simulation_id = await req.createSimulation(request, simulation);
    expect(simulation_id).not.toBeNull();
    expect(simulation_id).toBeDefined();
    expect(typeof simulation_id).toEqual("number");
  });

  //TODO: check EDSL for constraints - planConstraintsDslTypescript, modelConstraintsDslTypescript
  test('Check eDSL for plan', async ({ request }) => {
    //make sure it has externalProfile1 in it
    const plan_edsl = await req.getPlanConstraintsDsl(request, plan_id);
    expect(plan_edsl).not.toBeNull();
    expect(plan_edsl).toBeDefined();
    for (let elem of plan_edsl) {
      if (elem.filePath === 'mission-model-generated-code.ts') {
        expect(elem.content.includes("externalProfile1")).toBeTruthy();
      }
    }
  });

  test('Check eDSL for mission model', async ({ request }) => {
    //make sure it doesn't have externalProfile1 in it
    const mm_edsl = await req.getModelConstraintsDsl(request, mission_model_id.toString());
    expect(mm_edsl).not.toBeNull();
    expect(mm_edsl).toBeDefined();
    for (let elem of mm_edsl) {
      if (elem.filePath === 'mission-model-generated-code.ts') {
        expect(elem.content.includes("externalProfile1")).toBeFalsy();
      }
    }
  });


  test('Add Constraints', async ({request}) => {

    const constraints: Constraint[] = [
      {
        definition:"export default (): Constraint => Real.Resource(\"/plant\").greaterThan(196.0)",
        description:"test1",
        model_id:mission_model_id,
        name:"MMR.MC",
        plan_id:null,
        summary:"Mission Model Resource, Mission Constraint"
      },
      /*{
        definition:"export default (): Constraint => {Real.Resource(\"/externalProfile1\").greaterThan(90.0)}",
        description:"test2",
        model_id:mission_model_id,
        name:"PR.MC",
        plan_id:null,
        summary:"Plan Resource, Mission Constraint"
      },*/
      {
        definition:"export default (): Constraint => {Real.Resource(\"/plant\").greaterThan(196.0)}",
        description:"test3",
        model_id:null,
        name:"MMR.PC",
        plan_id:plan_id,
        summary:"Mission Model Resource, Plan Constraint"
      },
      {
        definition:"export default (): Constraint => {Real.Resource(\"/externalProfile1\").greaterThan(90.0)}",
        description:"test4",
        model_id:null,
        name:"PR.PC",
        plan_id:plan_id,
        summary:"PlanResource, Plan Constraint"
      }/*,
      {
        definition:"export default (): Constraint => {\n\n}",
        description:"sad",
        model_id:1,
        name:"please help me",
        plan_id:null,
        summary:"asd"
      },
      {
        definition:"export default (): Constraint => {\n\n}",
        description:"sad",
        model_id:1,
        name:"please help me",
        plan_id:null,
        summary:"asd"
      }*/
    ];
    for (const constraint of constraints) {
      const simulation_id = await req.addConstraint(request, constraint);
      expect(simulation_id).not.toBeNull();
      expect(simulation_id).toBeDefined();
      expect(typeof simulation_id).toEqual("number");
    }
  });


  //TODO: compile/check constraints - getViolations
  test('Compile/Get Violations', async ({ request }) => {
    console.log(await req.getViolations(request, plan_id));
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
