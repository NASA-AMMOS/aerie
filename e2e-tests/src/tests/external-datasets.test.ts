import { expect, test } from '@playwright/test';
import req from '../utilities/requests.js';
import time from '../utilities/time.js';

test.describe.serial('External Datasets', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T12:00:00.000";
  const dataset_start_timestamp = "2021-001T06:00:00.000";
  const profile_set = {
    "/my_boolean": {
      type: "discrete",
      schema: {
        type: "boolean"
      },
      segments: [
        {duration: 3600000000, "dynamics": false},
        {duration: 3600000000},
        {duration: 3600000000, "dynamics": true},
        {duration: 3600000000},
        {duration: 3600000000, "dynamics": false}
      ]
    }
  };

  const profile_set_extension_1 = {
    "/my_boolean": {
      type: "discrete",
      schema: {
        type: "boolean"
      },
      segments: [
        {duration: 1800000000, "dynamics": false},
        {duration: 1800000000, "dynamics": true},
      ]
    },
    "/new_profile": {
      type: "discrete",
      schema: {
        type: "boolean"
      },
      segments: [
        {duration: 1800000000, "dynamics": true},
        {duration: 1800000000, "dynamics": false},
        {duration: 1800000000},
        {duration: 1800000000, "dynamics": true}
      ]
    }
  };

  const profile_set_extension_2 = {
    "/newer_profile": {
      type: "discrete",
      schema: {
        type: "boolean"
      },
      segments: [
        {duration: 1800000000, "dynamics": true},
        {duration: 1800000000, "dynamics": false},
        {duration: 1800000000},
        {duration: 1800000000, "dynamics": true}
      ]
    }
  };

  const profile_set_result_1 = {
    plan_dataset_by_pk: {
      offset_from_plan_start: "06:00:00",
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: "00:00:00",
                dynamics: false
              },
              {
                start_offset: "01:00:00",
                dynamics: null
              },
              {
                start_offset: "02:00:00",
                dynamics: true
              },
              {
                start_offset: "03:00:00",
                dynamics: null
              },
              {
                start_offset: "04:00:00",
                dynamics: false
              }
            ]
          }
        ]
      }
    }
  };

  const profile_set_result_2 = {
    plan_dataset_by_pk: {
      offset_from_plan_start: "06:00:00",
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: "00:00:00",
                dynamics: false
              },
              {
                start_offset: "01:00:00",
                dynamics: null
              },
              {
                start_offset: "02:00:00",
                dynamics: true
              },
              {
                start_offset: "03:00:00",
                dynamics: null
              },
              {
                start_offset: "04:00:00",
                dynamics: false
              },
              {
                start_offset: "05:00:00",
                dynamics: false
              },
              {
                start_offset: "05:30:00",
                dynamics: true
              }
            ]
          },
          {
            profile_segments: [
              {
                start_offset: "00:00:00",
                dynamics: true
              },
              {
                start_offset: "00:30:00",
                dynamics: false
              },
              {
                start_offset: "01:00:00",
                dynamics: null
              },
              {
                start_offset: "01:30:00",
                dynamics: true
              }
            ]
          }
        ]
      }
    }
  };

  const profile_set_result_3 = {
    plan_dataset_by_pk: {
      offset_from_plan_start: "06:00:00",
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: "00:00:00",
                dynamics: false
              },
              {
                start_offset: "01:00:00",
                dynamics: null
              },
              {
                start_offset: "02:00:00",
                dynamics: true
              },
              {
                start_offset: "03:00:00",
                dynamics: null
              },
              {
                start_offset: "04:00:00",
                dynamics: false
              },
              {
                start_offset: "05:00:00",
                dynamics: false
              },
              {
                start_offset: "05:30:00",
                dynamics: true
              }
            ]
          },
          {
            profile_segments: [
              {
                start_offset: "00:00:00",
                dynamics: true
              },
              {
                start_offset: "00:30:00",
                dynamics: false
              },
              {
                start_offset: "01:00:00",
                dynamics: null
              },
              {
                start_offset: "01:30:00",
                dynamics: true
              }
            ]
          },
          {
            profile_segments: [
              {
                start_offset: "00:00:00",
                dynamics: true
              },
              {
                start_offset: "00:30:00",
                dynamics: false
              },
              {
                start_offset: "01:00:00",
                dynamics: null
              },
              {
                start_offset: "01:30:00",
                dynamics: true
              }
            ]
          }
        ]
      }
    }
  };

  let jar_id: number;
  let mission_model_id: number;
  let plan_id: number;
  let dataset_id: number;

  test('Create mission model and plan', async ({ request }) => {
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

  test('Upload External Dataset', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetInsertInput = {
      plan_id,
      dataset_start: dataset_start_timestamp,
      profile_set
    };

    dataset_id = await req.insertExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(typeof dataset_id).toEqual("number");
  });

  test('Query External Dataset 1', async ({ request }) => {
    const getExternalDatasetInput: ExternalDatasetQueryInput = { plan_id, dataset_id };

    const result = await req.getExternalDataset(request, getExternalDatasetInput);

    expect(result).toEqual(profile_set_result_1);
  });

  test('Extend External Dataset 1', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetExtendInput = {
      dataset_id,
      profile_set: profile_set_extension_1
    };

    dataset_id = await req.extendExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(typeof dataset_id).toEqual("number");
  });

  test('Query External Dataset 2', async ({ request }) => {
    const getExternalDatasetInput: ExternalDatasetQueryInput = { plan_id, dataset_id };

    const result = await req.getExternalDataset(request, getExternalDatasetInput);

    expect(result).toEqual(profile_set_result_2);
  });

  test('Extend External Dataset 2', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetExtendInput = {
      dataset_id,
      profile_set: profile_set_extension_2
    };

    dataset_id = await req.extendExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(typeof dataset_id).toEqual("number");
  });

  test('Query External Dataset 3', async ({ request }) => {
    const getExternalDatasetInput: ExternalDatasetQueryInput = { plan_id, dataset_id };

    const result = await req.getExternalDataset(request, getExternalDatasetInput);

    expect(result).toEqual(profile_set_result_3);
  });

  test('Delete External Dataset', async ({ request }) => {
    const input: ExternalDatasetQueryInput = { dataset_id, plan_id };

    const deleted_id = await req.deleteExternalDataset(request, input);
    expect(deleted_id).toEqual(dataset_id);
  });
});
