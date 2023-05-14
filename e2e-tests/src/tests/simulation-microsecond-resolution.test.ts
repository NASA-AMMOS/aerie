import { expect, test } from '@playwright/test';
import req from '../utilities/requests.js';
import time from "../utilities/time.js";

test.describe('Simulation microsecond resolution', () => {
  test('GrowBanana with duration of 1 microsecond should finish with a successful simulation', async ({ request }) => {
    const rd = Math.random() * 100;
    const plan_start_timestamp = "2021-001T00:00:00.000";
    const plan_end_timestamp = "2021-001T12:00:00.000";
    let jar_id: number;
    let mission_model_id: number;
    jar_id = await req.uploadJarFile(request);
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
    const plan_id = await req.createPlan(request, plan_input);
    const activityToInsert : ActivityInsertInput =
        {
          arguments : {
            "duration": 1
          },
          plan_id: plan_id,
          type : "ControllableDurationActivity",
          start_offset : "1h"
        };
    const simulationBounds : UpdateSimulationBoundsInput = {
      plan_id : plan_id,
      simulation_start_time: plan_start_timestamp,
      simulation_end_time: plan_end_timestamp
    };

    const max_it = 10;
    let it = 0;
    await req.insertActivity(request, activityToInsert);
    await req.updateSimulationBounds(request, simulationBounds);
    while (it++ < max_it) {
      const { reason, status, simulationDatasetId } = await req.simulate(request, plan_id);
      if (!(status == "pending" || status == "incomplete")) {
        if (!(status === "complete")) {
          console.error(JSON.stringify({reason}, undefined, 4));
          expect(status).toEqual("complete");
        }
        return;
      }
      await delay(1000);
    }
    throw Error("Simulation timed out after " + max_it + " iterations");
  });
});

function delay(ms: number) {
  return new Promise( resolve => setTimeout(resolve, ms) );
}
