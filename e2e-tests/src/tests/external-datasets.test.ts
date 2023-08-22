import { expect, test } from '@playwright/test';
import req, { awaitSimulation } from '../utilities/requests.js';
import time from '../utilities/time.js';

test.describe.serial('External Datasets', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = '2021-001T00:00:00.000';
  const plan_end_timestamp = '2021-001T12:00:00.000';
  const dataset_start_timestamp = '2021-001T06:00:00.000';
  const profile_set = {
    '/my_boolean': {
      type: 'discrete',
      schema: {
        type: 'boolean',
      },
      segments: [
        { duration: 3600000000, dynamics: false },
        { duration: 3600000000 },
        { duration: 3600000000, dynamics: true },
        { duration: 3600000000 },
        { duration: 3600000000, dynamics: false },
      ],
    },
  };

  const profile_set_extension_1 = {
    '/my_boolean': {
      type: 'discrete',
      schema: {
        type: 'boolean',
      },
      segments: [
        { duration: 1800000000, dynamics: false },
        { duration: 1800000000, dynamics: true },
      ],
    },
    '/new_profile': {
      type: 'discrete',
      schema: {
        type: 'boolean',
      },
      segments: [
        { duration: 1800000000, dynamics: true },
        { duration: 1800000000, dynamics: false },
        { duration: 1800000000 },
        { duration: 1800000000, dynamics: true },
      ],
    },
  };

  const profile_set_extension_2 = {
    '/newer_profile': {
      type: 'discrete',
      schema: {
        type: 'boolean',
      },
      segments: [
        { duration: 1800000000, dynamics: true },
        { duration: 1800000000, dynamics: false },
        { duration: 1800000000 },
        { duration: 1800000000, dynamics: true },
      ],
    },
  };

  const profile_set_result_1 = {
    plan_dataset_by_pk: {
      simulation_dataset_id: null,
      offset_from_plan_start: '06:00:00',
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: false,
              },
              {
                start_offset: '01:00:00',
                dynamics: null,
              },
              {
                start_offset: '02:00:00',
                dynamics: true,
              },
              {
                start_offset: '03:00:00',
                dynamics: null,
              },
              {
                start_offset: '04:00:00',
                dynamics: false,
              },
            ],
          },
        ],
      },
    },
  };

  const profile_set_result_2 = {
    plan_dataset_by_pk: {
      simulation_dataset_id: null,
      offset_from_plan_start: '06:00:00',
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: false,
              },
              {
                start_offset: '01:00:00',
                dynamics: null,
              },
              {
                start_offset: '02:00:00',
                dynamics: true,
              },
              {
                start_offset: '03:00:00',
                dynamics: null,
              },
              {
                start_offset: '04:00:00',
                dynamics: false,
              },
              {
                start_offset: '05:00:00',
                dynamics: false,
              },
              {
                start_offset: '05:30:00',
                dynamics: true,
              },
            ],
          },
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: true,
              },
              {
                start_offset: '00:30:00',
                dynamics: false,
              },
              {
                start_offset: '01:00:00',
                dynamics: null,
              },
              {
                start_offset: '01:30:00',
                dynamics: true,
              },
            ],
          },
        ],
      },
    },
  };

  const profile_set_result_3 = {
    plan_dataset_by_pk: {
      simulation_dataset_id: null,
      offset_from_plan_start: '06:00:00',
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: false,
              },
              {
                start_offset: '01:00:00',
                dynamics: null,
              },
              {
                start_offset: '02:00:00',
                dynamics: true,
              },
              {
                start_offset: '03:00:00',
                dynamics: null,
              },
              {
                start_offset: '04:00:00',
                dynamics: false,
              },
              {
                start_offset: '05:00:00',
                dynamics: false,
              },
              {
                start_offset: '05:30:00',
                dynamics: true,
              },
            ],
          },
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: true,
              },
              {
                start_offset: '00:30:00',
                dynamics: false,
              },
              {
                start_offset: '01:00:00',
                dynamics: null,
              },
              {
                start_offset: '01:30:00',
                dynamics: true,
              },
            ],
          },
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: true,
              },
              {
                start_offset: '00:30:00',
                dynamics: false,
              },
              {
                start_offset: '01:00:00',
                dynamics: null,
              },
              {
                start_offset: '01:30:00',
                dynamics: true,
              },
            ],
          },
        ],
      },
    },
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
      name: 'Banananation (e2e tests)' + rd,
      version: '0.0.0' + rd,
    };
    mission_model_id = await req.createMissionModel(request, model);
    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual('number');

    const plan_input: CreatePlanInput = {
      model_id: mission_model_id,
      name: 'test_plan' + rd,
      start_time: plan_start_timestamp,
      duration: time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp),
    };
    plan_id = await req.createPlan(request, plan_input);
    expect(plan_id).not.toBeNull();
    expect(plan_id).toBeDefined();
    expect(typeof plan_id).toEqual('number');
  });

  test('Upload External Dataset', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetInsertInput = {
      plan_id,
      dataset_start: dataset_start_timestamp,
      profile_set,
    };

    dataset_id = await req.insertExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(typeof dataset_id).toEqual('number');
  });

  test('Query External Dataset 1', async ({ request }) => {
    const getExternalDatasetInput: ExternalDatasetQueryInput = { plan_id, dataset_id };

    const result = await req.getExternalDataset(request, getExternalDatasetInput);

    expect(result).toEqual(profile_set_result_1);
  });

  test('Extend External Dataset 1', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetExtendInput = {
      dataset_id,
      profile_set: profile_set_extension_1,
    };

    dataset_id = await req.extendExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(typeof dataset_id).toEqual('number');
  });

  test('Query External Dataset 2', async ({ request }) => {
    const getExternalDatasetInput: ExternalDatasetQueryInput = { plan_id, dataset_id };

    const result = await req.getExternalDataset(request, getExternalDatasetInput);

    expect(result).toEqual(profile_set_result_2);
  });

  test('Extend External Dataset 2', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetExtendInput = {
      dataset_id,
      profile_set: profile_set_extension_2,
    };

    dataset_id = await req.extendExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(typeof dataset_id).toEqual('number');
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

test.describe.serial('Simulation Associated External Datasets', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = '2021-001T00:00:00.000';
  const plan_end_timestamp = '2021-001T12:00:00.000';
  const dataset_start_timestamp = '2021-001T06:00:00.000';
  const constraint_name = 'my_boolean is true';
  const profile_duration = 3600000000;
  const profile_set = {
    '/my_boolean': {
      type: 'discrete',
      schema: {
        type: 'boolean',
      },
      segments: [
        { duration: profile_duration, dynamics: true },
        { duration: profile_duration, dynamics: false },
      ],
    },
  };

  let profile_set_result = {
    plan_dataset_by_pk: {
      simulation_dataset_id: -1,
      offset_from_plan_start: '06:00:00',
      dataset: {
        profiles: [
          {
            profile_segments: [
              {
                start_offset: '00:00:00',
                dynamics: true,
              },
              {
                start_offset: '01:00:00',
                dynamics: false,
              },
            ],
          },
        ],
      },
    },
  };

  let jar_id: number;
  let mission_model_id: number;
  let plan_id: number;
  let dataset_id: number;
  let first_simulation_dataset_id: number;
  let second_simulation_dataset_id: number;
  let constraint_id: number;
  let constraintResult: ConstraintResult;
  let activity_id: number;

  test('Create mission model and plan', async ({ request }) => {
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
    expect(typeof mission_model_id).toEqual('number');

    const plan_input: CreatePlanInput = {
      model_id: mission_model_id,
      name: 'test_plan' + rd,
      start_time: plan_start_timestamp,
      duration: time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp),
    };
    plan_id = await req.createPlan(request, plan_input);
    expect(plan_id).not.toBeNull();
    expect(plan_id).toBeDefined();
    expect(typeof plan_id).toEqual('number');
  });

  test('Add Constraint to Plan', async ({ request }) => {
    const constraint: ConstraintInsertInput = {
      name: constraint_name,
      definition: 'export default (): Constraint => Discrete.Resource("/my_boolean").equal(true)',
      description: '',
      model_id: null,
      plan_id,
    };
    constraint_id = await req.insertConstraint(request, constraint);

    expect(constraint_id).not.toBeNull();
    expect(constraint_id).toBeDefined();
  });

  test('Run simulation', async ({ request }) => {
    const resp: SimulationResponse = await awaitSimulation(request, plan_id);
    first_simulation_dataset_id = resp.simulationDatasetId;

    expect(resp.status).toEqual('complete');
  });

  test('Upload Simulation Associated External Dataset', async ({ request }) => {
    const externalDatasetInput: ExternalDatasetInsertInput = {
      plan_id,
      simulation_dataset_id: first_simulation_dataset_id,
      dataset_start: dataset_start_timestamp,
      profile_set,
    };

    dataset_id = await req.insertExternalDataset(request, externalDatasetInput);
    expect(dataset_id).not.toBeNull();
    expect(dataset_id).toBeDefined();
    expect(dataset_id).not.toEqual(first_simulation_dataset_id);
    expect(typeof dataset_id).toEqual('number');
  });

  test('Query External Dataset', async ({ request }) => {
    profile_set_result.plan_dataset_by_pk.simulation_dataset_id = first_simulation_dataset_id;
    const getExternalDatasetInput: ExternalDatasetQueryInput = { plan_id, dataset_id };

    const result = await req.getExternalDataset(request, getExternalDatasetInput);

    expect(result).toEqual(profile_set_result);
  });

  test("Check there is one violation when simulationDatasetId isn't provided", async ({ request }) => {
    const constraintResults: ConstraintResult[] = await req.checkConstraints(request, plan_id);

    expect(constraintResults).not.toBeNull();
    expect(constraintResults).toBeDefined();
    expect(constraintResults).toHaveLength(1);

    constraintResult = constraintResults[0];
    expect(constraintResult).not.toBeNull();
    expect(constraintResult).toBeDefined();
  });

  test('Check there is one violation when simulationDatasetId is provided', async ({ request }) => {
    const constraintResults: ConstraintResult[] = await req.checkConstraints(request, plan_id, first_simulation_dataset_id);


    expect(constraintResults).not.toBeNull();
    expect(constraintResults).toBeDefined();
    expect(constraintResults).toHaveLength(1);

    expect(constraintResult).toEqual(constraintResults[0]); // should be the same as the violation from the prev test
    constraintResult = constraintResults[0];
    expect(constraintResult).not.toBeNull();
    expect(constraintResult).toBeDefined();
  });

  test('Check the violation is the expected one', async () => {
    expect(constraintResult.constraintName).toEqual(constraint_name);
    expect(constraintResult.constraintId).toEqual(constraint_id);
    expect(constraintResult.resourceIds).toHaveLength(1);
    expect(constraintResult.resourceIds).toContain('/my_boolean');
  });

  test('Check violation starts and ends as expected', async () => {
    const plan_start_unix = 1000 * time.getUnixEpochTime(plan_start_timestamp);
    const dataset_start_unix = 1000 * time.getUnixEpochTime(dataset_start_timestamp);

    expect(constraintResult.violations[0].violationIntervals[0].start).toEqual(dataset_start_unix - plan_start_unix + profile_duration);
    expect(constraintResult.violations[0].violationIntervals[0].end).toEqual(dataset_start_unix - plan_start_unix + 2 * profile_duration);
  });

  test('Add activity to plan to make simulation out of date', async ({ request }) => {
    const activityToInsert: ActivityInsertInput = {
      arguments: {
        biteSize: 1,
      },
      plan_id: plan_id,
      type: 'BiteBanana',
      start_offset: '1h',
    };
    activity_id = await req.insertActivity(request, activityToInsert);

    expect(activity_id).not.toBeNull();
    expect(activity_id).toBeDefined();
  });

  test('Run new simulation', async ({ request }) => {
    const resp: SimulationResponse = await awaitSimulation(request, plan_id);
    second_simulation_dataset_id = resp.simulationDatasetId;

    expect(resp.status).toEqual('complete');
  });

  test("Check there is still one violation when simulationDatasetId isn't provided", async ({ request }) => {
    const violations: ConstraintResult[] = await req.checkConstraints(request, plan_id);

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(1);
  });

  test('Check that the first constraint run is not marked as outdated when a new simulation has been run', async ({
    request,
  }) => {
    const constraintRuns: ConstraintRun[] = await req.getConstraintRuns(request, first_simulation_dataset_id);

    expect(constraintRuns).not.toBeNull();
    expect(constraintRuns).toBeDefined();
    expect(constraintRuns).toHaveLength(1);
    expect(constraintRuns[0].definition_outdated).toEqual(false);
  });

  test('Check there is still one violation when the first simulationDatasetId is provided', async ({ request }) => {
    const violations: ConstraintResult[] = await req.checkConstraints(request, plan_id, first_simulation_dataset_id);

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(1);
  });

  test('Check there are violations when second simulationDatasetId is provided', async ({ request }) => {
    const violations: ConstraintResult[] = await req.checkConstraints(
      request,
      plan_id,
      second_simulation_dataset_id,
    );

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(1);
  });

  test('Check constraint is deleted', async ({ request }) => {
    const id = await req.deleteConstraint(request, constraint_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(constraint_id);
  });

  test('Delete External Dataset', async ({ request }) => {
    const input: ExternalDatasetQueryInput = { dataset_id, plan_id };

    const deleted_id = await req.deleteExternalDataset(request, input);
    expect(deleted_id).toEqual(dataset_id);
  });

  test('Check plan is deleted', async ({ request }) => {
    const id = await req.deletePlan(request, plan_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(plan_id);
  });

  test('Check model is deleted', async ({ request }) => {
    const id = await req.deleteMissionModel(request, mission_model_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(mission_model_id);
  });
});
