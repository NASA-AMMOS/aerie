import { expect, test } from '@playwright/test';
import req from '../utilities/requests';
import time from '../utilities/time';
import tests from '../utilities/constraints-checking-sparse-data';

test.describe('Sparse', () => {
  const test_id = "test1";

  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T00:01:00.000";
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
          start_offset: "00:00:15.000",
          type: "BakeBananaBread"
        },
        {
          arguments: {
            temperature: 375.0,
            tbSugar: 3,
            glutenFree: false
          },
          plan_id: plan_id,
          start_offset: "00:00:30.000",
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
        datasetStart: "2021-001T00:00:14",
        profileSet: `{
          externalProfile1: {
            type: "real",
            segments: [
              {
                duration: 15000000,
                dynamics: {
                  initial: 50,
                  rate: -1
                }
              },
              {
                duration: 3000000,
                dynamics: {
                  initial: 35,
                  rate: -0.5
                }
              }
            ]
          }
        }`
      },
      { //attempt to make sparse dataset for externalProfile1... fails and overwrites, and datasetStart doesn't even work
        plan_id: plan_id,
        datasetStart: "2021-001T00:00:45",
        profileSet: `{
          externalProfile1: {
            type: "real",
            segments: [
              {
                duration: 5000000,
                dynamics: {
                  initial: 20,
                  rate: -1
                }
              },
              {
                duration: 5000000,
                dynamics: {
                  initial: 69,
                  rate: -0.5
                }
              }
            ]
          }
        }`
      },
      {
        plan_id: plan_id,
        datasetStart: "2021-001T00:00:10",
        profileSet: `{
          externalProfile2: {
            type: "discrete",
            schema: {
              type: "boolean"
            },
            segments: [
              {
                duration: 20000000,
                dynamics: true
              },
              {
                duration: 20000000,
                dynamics: false
              }
            ]
          }
        }`
      }
    ];


    for (const profile of profiles) {
      const dataset_id = await req.addExternalProfile(request, profile);
      expect(dataset_id).not.toBeNull();
      expect(dataset_id).toBeDefined();
      expect(typeof dataset_id).toEqual("number");
    }
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

  test('Run Simulation', async ({request}) => {
    var simulation_status;
    do {
      simulation_status = await req.runSimulation(request, plan_id);
      expect(simulation_status).not.toBeNull();
      expect(simulation_status).toBeDefined();
      console.log(simulation_status);
    }
    while (simulation_status === "pending" || simulation_status === "incomplete")
  });

  test('Add Sparse Constraints', async ({request}) => {
    const constraints: Constraint[] = [
      {
        definition:"export default (): Constraint => Real.Resource(\"externalProfile1\").greaterThan(25.0)",
        description:"test4",
        model_id:null,
        name:"PR.PC",
        plan_id:plan_id,
        summary:"PlanResource, Plan Constraint"
      }
    ];
    for (const constraint of constraints) {
      const simulation_id = await req.addConstraint(request, constraint);
      expect(simulation_id).not.toBeNull();
      expect(simulation_id).toBeDefined();
      expect(typeof simulation_id).toEqual("number");
    }
  });

  test('Compile/Check Sparse Constraints', async ({ request }) => {
    const constraint_violations = await req.getViolations(request, plan_id);
    console.log(constraint_violations);
    console.log(JSON.stringify(constraint_violations))
    expect(constraint_violations).not.toBeNull();
    expect(constraint_violations).toBeDefined();
    expect(constraint_violations["plan/PR.PC"]).not.toBeNull();
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
