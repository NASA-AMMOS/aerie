import { expect, test } from '@playwright/test';
import req from '../utilities/requests.js';
import time from "../utilities/time.js";


/*
  Tests various simulation and simulation template start and end time configurations.
  On all tests, unless otherwise specified, these are the following values:
  - Simulation's simulation_start_time: Plan Start (January 1, 2023, at 00:00:00 Z)
  - Simulation's simulation_end_time: Halfway through plan (January 1, 2023, at 12:00:00 Z)
  - Simulation Template's simulation_start_time: Halfway through plan (January 1, 2023, at 12:00:00 Z)
  - Simulation Template's simulation_end_time: Plan End (January 2, 2023, at 00:00:00 Z)
*/

test.describe('Temporal Subset Simulation Config and Template Inputs: Exception Cases', () => {
  const plan_start_timestamp = "2023-01-01T00:00:00+00:00";
  const midway_plan_timestamp = "2023-01-01T12:00:00+00:00";
  const plan_end_timestamp = "2023-01-02T00:00:00+00:00";

  let mission_model_id: number;
  let plan_id: number;
  let simulation_id: number;
  let simulation_template_id: number;

  test.beforeEach(async ({ request }) => {
    let rd = Math.random()*100000;
    let jar_id = await req.uploadJarFile(request);
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: rd + "",
    };
    mission_model_id = await req.createMissionModel(request, model);
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input);
    simulation_id = await req.getSimulationId(request, plan_id);
    const simulation_template : InsertSimulationTemplateInput = {
      model_id: mission_model_id,
      arguments: {},
      description: 'Template for Plan ' +plan_id
    };
    simulation_template_id = await req.insertAndAssociateSimulationTemplate(request, simulation_template, simulation_id );
    const firstHalfActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 3600000000 //1hr
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "1h"
        };
    const midpointActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 7200000000 //2hrs
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "12h"
        };
    const secondHalfActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 10800000000 //3hrs
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "14h"
        };
    await req.insertActivity(request, firstHalfActivity);
    await req.insertActivity(request, midpointActivity);
    await req.insertActivity(request, secondHalfActivity);
  });
  test.afterEach(async ({ request }) => {
    // Delete Plan and Model cascades the rest of the cleanup
    await req.deleteMissionModel(request, mission_model_id);
    await req.deletePlan(request, plan_id);
  });

  // ~~~~~~~~ Exception Cases ~~~~~~~~ //
  test('Simulation start time, end time are null; Template start time, end time are null', async ({ request }) => {
    // Expect a 500 error bc of a RuntimeException
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time is defined, end time is null; Template start time, end time are null', async({request}) => {
    // Expect a 500 error bc of a RuntimeException
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: null
    };
    await req.updateSimulationBounds(request, simulationBounds);
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time is null, end time is defined; Template start time, end time are null', async({request}) => {
    // Expect a 500 error bc of a RuntimeException
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: null,
      simulation_end_time: midway_plan_timestamp
    };
    await req.updateSimulationBounds(request, simulationBounds);
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time, end time is null; Template start time is defined, end time is null', async({request}) => {
    // Expect a 500 error bc of a RuntimeException
    const templateBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: null
    };
    await req.updateSimulationTemplateBounds(request, templateBounds);
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time, end time is null; Template start time is null, end time is defined', async({request}) => {
    // Expect a 500 error bc of a RuntimeException
    const templateBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: null,
      simulation_end_time: plan_end_timestamp
    };
    await req.updateSimulationTemplateBounds(request, templateBounds);
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time is defined, end time is null; Template start time is defined, end time is null', async({request}) => {
    // Expect a 500 error bc of a RuntimeException
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: null
    };
    const templateBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: midway_plan_timestamp,
      simulation_end_time: null
    };
    await req.updateSimulationBounds(request, simulationBounds);
    await req.updateSimulationTemplateBounds(request, templateBounds);
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time is null, end time is defined; Template start time is null, end time is defined', async({request}) => {
    // Expect a 500 error bc of a RuntimeException
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: null,
      simulation_end_time: midway_plan_timestamp
    };
    const templateBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: null,
      simulation_end_time: plan_end_timestamp
    };
    await req.updateSimulationBounds(request, simulationBounds);
    await req.updateSimulationTemplateBounds(request, templateBounds);
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
});

test.describe('Temporal Subset Simulation Config and Template Inputs: Valid Cases', () => {
  const plan_start_timestamp = "2023-01-01T00:00:00+00:00";
  const midway_plan_timestamp = "2023-01-01T12:00:00+00:00";
  const plan_end_timestamp = "2023-01-02T00:00:00+00:00";

  let mission_model_id: number;
  let plan_id: number;
  let simulation_id: number;
  let simulation_template_id: number;
  let first_half_activity_id: number;
  let midpoint_activity_id: number;
  let second_half_activity_id: number;

  test.beforeEach(async ({ request }) => {
    let rd = Math.random()*100000;
    let jar_id = await req.uploadJarFile(request);
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: rd + "",
    };
    mission_model_id = await req.createMissionModel(request, model);
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input);
    simulation_id = await req.getSimulationId(request, plan_id);
    const simulation_template : InsertSimulationTemplateInput = {
      model_id: mission_model_id,
      arguments: {},
      description: 'Template for Plan ' +plan_id
    };
    simulation_template_id = await req.insertAndAssociateSimulationTemplate(request, simulation_template, simulation_id);
    const firstHalfActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 3600000000 //1hr
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "1h"
        };
    const midpointActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 7200000000 //2hrs
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "12h"
        };
    const secondHalfActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 10800000000 //3hrs
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "14h"
        };
    first_half_activity_id = await req.insertActivity(request, firstHalfActivity);
    midpoint_activity_id = await req.insertActivity(request, midpointActivity);
    second_half_activity_id = await req.insertActivity(request, secondHalfActivity);
  });
  test.afterEach(async ({ request }) => {
    // Deleting Plan and Model cascades the rest of the cleanup
    await req.deleteMissionModel(request, mission_model_id);
    await req.deletePlan(request, plan_id);
  });

  test('Simulation start time, end time are defined; Template start time, end time are null', async ({ request }) => {
    // Expect the first half of the plan to be simulated
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: midway_plan_timestamp
    };
    await req.updateSimulationBounds(request, simulationBounds);

    let simulationDatasetId = await awaitSimulation(request, plan_id);

    let {canceled, simulation_start_time, simulation_end_time, simulated_activities} = await req.getSimulationDataset(request, simulationDatasetId);
    expect(canceled).toEqual(false);
    expect(simulation_start_time).toEqual(plan_start_timestamp);
    expect(simulation_end_time).toEqual(midway_plan_timestamp);
    expect(simulated_activities.length).toEqual(2);

    simulated_activities.sort((firstElement, secondElement) => {return firstElement.activity_directive.id-secondElement.activity_directive.id});

    let unfinishedActivity = simulated_activities.pop();
    expect(unfinishedActivity.activity_directive.id).toEqual(midpoint_activity_id);
    expect(unfinishedActivity.duration).toEqual(null);
    expect(unfinishedActivity.start_offset).toEqual("12:00:00");
    expect(unfinishedActivity.start_time).toEqual(midway_plan_timestamp);

    let firstActivity = simulated_activities.pop();
    expect(firstActivity.activity_directive.id).toEqual(first_half_activity_id);
    expect(firstActivity.duration).toEqual("01:00:00");
    expect(firstActivity.start_offset).toEqual("01:00:00");
    expect(firstActivity.start_time).toEqual("2023-01-01T01:00:00+00:00");
  });
  test('Simulation start time, end time are null; Template start time, end time are defined', async ({ request }) => {
    // Expect the second half of the plan to be simulated
    const templateSimulationBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: midway_plan_timestamp,
      simulation_end_time: plan_end_timestamp
    };
    await req.updateSimulationTemplateBounds(request, templateSimulationBounds);

    let simulationDatasetId = await awaitSimulation(request, plan_id);

    let {canceled, simulation_start_time, simulation_end_time, simulated_activities} = await req.getSimulationDataset(request, simulationDatasetId);
    expect(canceled).toEqual(false);
    expect(simulation_start_time).toEqual(midway_plan_timestamp);
    expect(simulation_end_time).toEqual(plan_end_timestamp);
    expect(simulated_activities.length).toEqual(2);

    simulated_activities.sort((firstElement, secondElement) => {return firstElement.activity_directive.id-secondElement.activity_directive.id});

    let secondActivity = simulated_activities.pop();
    expect(secondActivity.activity_directive.id).toEqual(second_half_activity_id);
    expect(secondActivity.duration).toEqual("03:00:00");
    expect(secondActivity.start_offset).toEqual("02:00:00");
    expect(secondActivity.start_time).toEqual("2023-01-01T14:00:00+00:00");

    let firstActivity = simulated_activities.pop();
    expect(firstActivity.activity_directive.id).toEqual(midpoint_activity_id);
    expect(firstActivity.duration).toEqual("02:00:00");
    expect(firstActivity.start_offset).toEqual("00:00:00");
    expect(firstActivity.start_time).toEqual(midway_plan_timestamp);
  });
  test('Simulation start time is defined, end time is null; Template start time is null, end time are defined', async ({ request }) => {
    // Expect the entire plan to be simulated
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: null
    };
    const templateSimulationBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: null,
      simulation_end_time: plan_end_timestamp
    };
    await req.updateSimulationBounds(request, simulationBounds);
    await req.updateSimulationTemplateBounds(request, templateSimulationBounds);

    let simulationDatasetId = await awaitSimulation(request, plan_id);

    let {canceled, simulation_start_time, simulation_end_time, simulated_activities} = await req.getSimulationDataset(request, simulationDatasetId);
    expect(canceled).toEqual(false);
    expect(simulation_start_time).toEqual(plan_start_timestamp);
    expect(simulation_end_time).toEqual(plan_end_timestamp);
    expect(simulated_activities.length).toEqual(3);

    simulated_activities.sort((firstElement, secondElement) => {return firstElement.activity_directive.id-secondElement.activity_directive.id});

    let thirdActivity = simulated_activities.pop();
    expect(thirdActivity.activity_directive.id).toEqual(second_half_activity_id);
    expect(thirdActivity.duration).toEqual("03:00:00");
    expect(thirdActivity.start_offset).toEqual("14:00:00");
    expect(thirdActivity.start_time).toEqual("2023-01-01T14:00:00+00:00");

    let secondActivity = simulated_activities.pop();
    expect(secondActivity.activity_directive.id).toEqual(midpoint_activity_id);
    expect(secondActivity.duration).toEqual("02:00:00");
    expect(secondActivity.start_offset).toEqual("12:00:00");
    expect(secondActivity.start_time).toEqual(midway_plan_timestamp);

    let firstActivity = simulated_activities.pop();
    expect(firstActivity.activity_directive.id).toEqual(first_half_activity_id);
    expect(firstActivity.duration).toEqual("01:00:00");
    expect(firstActivity.start_offset).toEqual("01:00:00");
    expect(firstActivity.start_time).toEqual("2023-01-01T01:00:00+00:00");
  });
  test('Simulation start time is null, end time is defined; Template start time is defined, end time is null', async({request}) => {
    // Expect the simulation to have no duration
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: null,
      simulation_end_time: midway_plan_timestamp
    };
    const templateSimulationBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: midway_plan_timestamp,
      simulation_end_time: null
    };
    await req.updateSimulationBounds(request, simulationBounds);
    await req.updateSimulationTemplateBounds(request, templateSimulationBounds);

    let simulationDatasetId = await awaitSimulation(request, plan_id);

    let {canceled, simulation_start_time, simulation_end_time, simulated_activities} = await req.getSimulationDataset(request, simulationDatasetId);
    expect(canceled).toEqual(false);
    expect(simulation_start_time).toEqual(midway_plan_timestamp);
    expect(simulation_end_time).toEqual(midway_plan_timestamp);
    expect(simulated_activities.length).toEqual(1);

    let unfinishedActivity = simulated_activities.pop();
    expect(unfinishedActivity.activity_directive.id).toEqual(midpoint_activity_id);
    expect(unfinishedActivity.duration).toEqual(null);
    expect(unfinishedActivity.start_offset).toEqual("00:00:00");
    expect(unfinishedActivity.start_time).toEqual(midway_plan_timestamp);
  });
  test('Simulation start time, end time is defined; Template start time, end time is defined', async({request}) => {
    /*
      In this test:
        - Simulation's simulation_start_time: (January 1, 2023, at 02:00:00 Z)
        - Simulation's simulation_end_time: (January 1, 2023, at 13:00:00 Z)
    */
    // Expect the simulation to be 11 Hours long (02:00:00 Z to 13:00:00 Z)
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: "2023-01-01T02:00:00+00:00",
      simulation_end_time: "2023-01-01T13:00:00+00:00"
    };
    const templateSimulationBounds : UpdateSimulationTemplateBoundsInput = {
      simulation_template_id : simulation_template_id,
      simulation_start_time: midway_plan_timestamp,
      simulation_end_time: plan_end_timestamp
    };

    await req.updateSimulationBounds(request, simulationBounds);
    await req.updateSimulationTemplateBounds(request, templateSimulationBounds);

    let simulationDatasetId = await awaitSimulation(request, plan_id);

    let {canceled, simulation_start_time, simulation_end_time, simulated_activities} = await req.getSimulationDataset(request, simulationDatasetId);
    expect(canceled).toEqual(false);
    expect(simulation_start_time).toEqual(simulationBounds.simulation_start_time);
    expect(simulation_end_time).toEqual(simulationBounds.simulation_end_time);
    expect(simulated_activities.length).toEqual(1);

    let unfinishedActivity = simulated_activities.pop();
    expect(unfinishedActivity.activity_directive.id).toEqual(midpoint_activity_id);
    expect(unfinishedActivity.duration).toEqual(null);
    expect(unfinishedActivity.start_offset).toEqual("10:00:00");
    expect(unfinishedActivity.start_time).toEqual(midway_plan_timestamp);
  });
});

test.describe('Temporal Subset Simulation Config: No Template', () => {
  const plan_start_timestamp = "2023-01-01T00:00:00+00:00";
  const midway_plan_timestamp = "2023-01-01T12:00:00+00:00";
  const plan_end_timestamp = "2023-01-02T00:00:00+00:00";

  let mission_model_id: number;
  let plan_id: number;
  let simulation_id: number;
  let first_half_activity_id: number;
  let midpoint_activity_id: number;

  test.beforeEach(async ({ request }) => {
    let rd = Math.random()*100000;
    let jar_id = await req.uploadJarFile(request);
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: rd + "",
    };
    mission_model_id = await req.createMissionModel(request, model);
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input);
    simulation_id = await req.getSimulationId(request, plan_id);

    const firstHalfActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 3600000000 //1hr
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "1h"
        };
    const midpointActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 7200000000 //2hrs
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "12h"
        };
    const secondHalfActivity : ActivityInsertInput =
        {
          arguments : {
            "duration": 10800000000 //3hrs
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "14h"
        };
    first_half_activity_id = await req.insertActivity(request, firstHalfActivity);
    midpoint_activity_id = await req.insertActivity(request, midpointActivity);
    await req.insertActivity(request, secondHalfActivity);
  });
  test.afterEach(async ({ request }) => {
    // Delete Plan and Model cascades the rest of the cleanup
    await req.deleteMissionModel(request, mission_model_id);
    await req.deletePlan(request, plan_id);
  });
  test('Simulation start time, end time are null', async ({ request }) => {
    // Expect a 500 error bc of a RuntimeException
    try{
      await req.simulate(request, plan_id);
      test.fail();
    } catch(error) {
      const err = error as Error;
      expect(err.message).toEqual("internal error");
    }
  });
  test('Simulation start time, end time are defined', async ({ request }) => {
    // Expect the first half of the plan to be simulated
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: midway_plan_timestamp
    };
    await req.updateSimulationBounds(request, simulationBounds);

    let simulationDatasetId = await awaitSimulation(request, plan_id);

    let {canceled, simulation_start_time, simulation_end_time, simulated_activities} = await req.getSimulationDataset(request, simulationDatasetId);
    expect(canceled).toEqual(false);
    expect(simulation_start_time).toEqual(plan_start_timestamp);
    expect(simulation_end_time).toEqual(midway_plan_timestamp);
    expect(simulated_activities.length).toEqual(2);

    simulated_activities.sort((firstElement, secondElement) => {return firstElement.activity_directive.id-secondElement.activity_directive.id});

    let unfinishedActivity = simulated_activities.pop();
    expect(unfinishedActivity.activity_directive.id).toEqual(midpoint_activity_id);
    expect(unfinishedActivity.duration).toEqual(null);
    expect(unfinishedActivity.start_offset).toEqual("12:00:00");
    expect(unfinishedActivity.start_time).toEqual(midway_plan_timestamp);

    let firstActivity = simulated_activities.pop();
    expect(firstActivity.activity_directive.id).toEqual(first_half_activity_id);
    expect(firstActivity.duration).toEqual("01:00:00");
    expect(firstActivity.start_offset).toEqual("01:00:00");
    expect(firstActivity.start_time).toEqual("2023-01-01T01:00:00+00:00");
  });
});

function delay(ms: number) {
  return new Promise( resolve => setTimeout(resolve, ms) );
}

async function awaitSimulation(request, plan_id, ) {
  let it = 0;
  while (it++ < 10) {
    const { reason, status, simulationDatasetId } = await req.simulate(request, plan_id);
    if (!(status == "pending" || status == "incomplete")) {
      if (!(status === "complete")) {
        console.error(JSON.stringify({reason}, undefined, 4));
        expect(status).toEqual("complete");
      }
      return simulationDatasetId;
    }
    await delay(1000);
  }
  throw Error("Simulation timed out after " + 10 + " iterations");
}
