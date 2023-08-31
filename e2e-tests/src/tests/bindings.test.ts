/**
 * Test the Action Bindings for Merlin and the Scheduler
 *
 * Health endpoints are already tested in health.test.ts
 */
import { expect, test } from '@playwright/test';
import req, {awaitSimulation} from '../utilities/requests.js';
import time from "../utilities/time.js";
import * as urls from '../utilities/urls.js';

test.describe('Merlin Bindings', () => {
  let mission_model_id: number;
  let plan_id: number;
  let admin: User = {
    username: "Admin_User",
    default_role: "aerie_admin",
    allowed_roles: ["aerie_admin"],
    session: {'x-hasura-role': 'aerie_admin', 'x-hasura-user-id': 'Admin_User'}
  };
  let nonOwner: User = {
    username: "not_owner",
    default_role: "user",
    allowed_roles: ["user", "viewer"],
    session: {'x-hasura-role': 'user', 'x-hasura-user-id': 'not_owner'}
  };

  test.beforeAll(async ({ request }) => {
    // Insert the users
    await req.createUser(request, admin);
    await req.createUser(request, nonOwner);

    // Insert the Mission Model
    let rd = Math.random()*100000;
    let jar_id = await req.uploadJarFile(request);
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: rd + "",
    };
    mission_model_id = await req.createMissionModel(request, model);

    const plan_start_timestamp = "2023-01-01T00:00:00+00:00";
    const plan_end_timestamp = "2023-01-02T00:00:00+00:00";
    // Insert the Plan
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan_' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input, admin.session);
  });
  test.afterAll(async ({ request }) => {
    // Remove Model and Plan
    await req.deleteMissionModel(request, mission_model_id);
    await req.deletePlan(request, plan_id);

    // Remove Users
    await req.deleteUser(request, admin.username);
    await req.deleteUser(request, nonOwner.username);
  });

  // "resourceTypes" and "getActivityEffectiveArguments" are not tested, as they are deprecated
  test('GetSimulationResults', async ({request}) => {
    // Create a custom plan for this test, so that sim results don't affect later tests
    const local_plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan_' + Math.random()*100000,
      start_time : "2023-01-01T00:00:00+00:00",
      duration : "24:00:00"
    };
    const local_plan_id = await req.createPlan(request, local_plan_input, admin.session);

    // Returns a 404 if the PlanId is invalid
    // message is "no such plan"
    let response = await request.post(`${urls.MERLIN_URL}/getSimulationResults`, {
      data: {
        action: {name: "simulate"},
        input: {planId: -1},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such plan')


    // Returns a 403 if Unauthorized
    response = await request.post(`${urls.MERLIN_URL}/getSimulationResults`, {
      data: {
        action: {name: "simulate"},
        input: {planId: local_plan_id},
        request_query: "",
        session_variables: nonOwner.session}});
    expect(response.status()).toEqual(403);
    expect((await response.json()).message).toEqual(
        "User '"+nonOwner.username+"' with role 'user' cannot perform 'simulate' because they are not " +
        "a 'PLAN_OWNER_COLLABORATOR' for plan with id '"+local_plan_id+"'");

    // Returns a 200 otherwise
    // "status" is not "failed"
    response = await request.post(`${urls.MERLIN_URL}/getSimulationResults`, {
      data: {
        action: {name: "simulate"},
        input: {planId: local_plan_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json()).status).not.toEqual('failed');

    // Cleanup sim results
    await awaitSimulation(request, local_plan_id);
    await req.deletePlan(request, local_plan_id)
  });
  test('ResourceSamples', async ({request}) => {
    // Returns a 404 if the PlanId is invalid
    // message is "no such plan"
    let response = await request.post(`${urls.MERLIN_URL}/resourceSamples`, {
      data: {
        action: {name: "resource_samples"},
        input: {planId: -1},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such plan')


    // 403: Unauthorized requires updating permissions
    const og_permissions = await req.getActionPermissionsForRole(request, "user");
    const temp_permissions: ActionPermissionSet = {
      resource_samples: "PLAN_OWNER",
      simulate: null,
      schedule: null,
      sequence_seq_json_bulk: null,
      check_constraints: null,
      create_expansion_rule: null,
      create_expansion_set: null,
      insert_ext_dataset: null,
      expand_all_activities: null,
    };
    await req.updateActionPermissionsForRole(request, "user", temp_permissions );

    response = await request.post(`${urls.MERLIN_URL}/resourceSamples`, {
      data: {
        action: {name: "resource_samples"},
        input: {planId: plan_id},
        request_query: "",
        session_variables: nonOwner.session}});
    expect(response.status()).toEqual(403);
    expect((await response.json()).message).toEqual(
        "User '"+nonOwner.username+"' with role 'user' cannot perform 'resource_samples' because " +
        "they are not a 'PLAN_OWNER' for plan with id '"+plan_id+"'");

    await req.updateActionPermissionsForRole(request, "user", og_permissions);
    expect(og_permissions).toEqual(await req.getActionPermissionsForRole(request, "user"));

    // Returns a 200 otherwise
    // "resourceSamples" is empty because there is no sim data
    response = await request.post(`${urls.MERLIN_URL}/resourceSamples`, {
      data: {
        action: {name: "resource_samples"},
        input: {planId: plan_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json()).resourceSamples).toEqual({});
  });
  test('ConstraintViolations', async ({request}) => {
    // Returns a 404 if the PlanId is invalid
    // message is "no such plan"
    let response = await request.post(`${urls.MERLIN_URL}/constraintViolations`, {
      data: {
        action: {name: "check_constraints"},
        input: {planId: -1},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such plan');

    // Returns a 403 if unauthorized
    response = await request.post(`${urls.MERLIN_URL}/constraintViolations`, {
      data: {
        action: {name: "check_constraints"},
        input: {planId: plan_id},
        request_query: "",
        session_variables: nonOwner.session}});
    expect(response.status()).toEqual(403);
    expect((await response.json()).message).toEqual(
        "User '"+nonOwner.username+"' with role 'user' cannot perform 'check_constraints' because they are not " +
        "a 'PLAN_OWNER_COLLABORATOR' for plan with id '"+plan_id+"'");

    // Returns a 404 if no simulation datasets are found
    response = await request.post(`${urls.MERLIN_URL}/constraintViolations`, {
      data: {
        action: {name: "check_constraints"},
        input: {planId: plan_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json())).toEqual({
      message: "input mismatch exception",
      cause: "no simulation datasets found for plan id " + plan_id
    });

    // Simulation already tested; run one
    // "status" is not "failed"
    response = await request.post(`${urls.MERLIN_URL}/getSimulationResults`, {
      data: {
        action: {name: "simulate"},
        input: {planId: plan_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json()).status).not.toEqual('failed');
    await awaitSimulation(request, plan_id);

    // Returns a 200 because a simulation dataset exists now
    // results are empty because there are no constraints that could've failed
    response = await request.post(`${urls.MERLIN_URL}/constraintViolations`, {
      data: {
        action: {name: "check_constraints"},
        input: {planId: plan_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json())).toEqual([]);
  });
  test('RefreshModelParameters', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/refreshModelParameters`, {
      data: { event: { data: {old: null, new: {id: -1}}}}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');

    // Returns a 200 if the ID is valid
    // There is no response body from this endpoint and awaiting it causes an "unexpected end of JSON input" error
    response = await request.post(`${urls.MERLIN_URL}/refreshModelParameters`, {
      data: { event: { data: {old: null, new: {id: mission_model_id}}}}});
    expect(response.status()).toEqual(200);
  });
  test('RefreshActivityTypes', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/refreshActivityTypes`, {
      data: { event: { data: {old: null, new: {id: -1}}}}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');

    // Returns a 200 if the ID is valid
    // There is no response body from this endpoint and awaiting it causes an "unexpected end of JSON input" error
    response = await request.post(`${urls.MERLIN_URL}/refreshActivityTypes`, {
      data: { event: { data: {old: null, new: {id: mission_model_id}}}}});
    expect(response.status()).toEqual(200);
  });
  test('RefreshResourceTypes', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/refreshResourceTypes`, {
      data: { event: { data: {old: null, new: {id: -1}}}}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');

    // Returns a 200 if the ID is valid
    // There is no response body from this endpoint and awaiting it causes an "unexpected end of JSON input" error
    response = await request.post(`${urls.MERLIN_URL}/refreshResourceTypes`, {
      data: { event: { data: {old: null, new: {id: mission_model_id}}}}});
    expect(response.status()).toEqual(200);
  });
  test('ValidateActivityArguments', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/validateActivityArguments`, {
      data: {
        action: {name: "validateActivityArguments"},
        input: { missionModelId: '-1', activityTypeName: "BiteBanana", activityArguments: {}},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');

    // Returns a 200 otherwise
    // "success" is true
    response = await request.post(`${urls.MERLIN_URL}/validateActivityArguments`, {
      data: {
        action: {name: "validateActivityArguments"},
        input: {missionModelId: ""+mission_model_id, activityTypeName: "BiteBanana", activityArguments: {}},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect(await response.json()).toEqual({ success:true });
  });
  test('ValidateModelArguments', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/validateModelArguments`, {
      data: {
        action: {name: "validateModelArguments"},
        input: { missionModelId: '-1', modelArguments: {}},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');


    // Returns a 200 if the ID is valid
    response = await request.post(`${urls.MERLIN_URL}/validateModelArguments`, {
      data: {
        action: {name: "validateModelArguments"},
        input: { missionModelId:''+mission_model_id, modelArguments: {}},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect(await response.json()).toEqual({ success:true });
  });
  test('ValidatePlan', async ({request}) => {
    // Returns a 404 if the PlanId is invalid
    // message is "no such plan"
    let response = await request.post(`${urls.MERLIN_URL}/validatePlan`, {
      data: {
        action: {name: "validatePlan"},
        input: { planId: -1},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such plan');

    // Returns a 200 otherwise
    response = await request.post(`${urls.MERLIN_URL}/validatePlan`, {
      data: {
        action: {name: "validatePlan"},
        input: { planId: plan_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect(await response.json()).toEqual({ success:true });
  });
  test('GetModelEffectiveArguments', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/getModelEffectiveArguments`, {
      data: {
        action: {name: "getModelEffectiveArguments"},
        input: { missionModelId: '-1', modelArguments: {}},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');

    // Returns a 200 otherwise
    // Body contains the complete set of args for the mission model (all default in this case)
    response = await request.post(`${urls.MERLIN_URL}/getModelEffectiveArguments`, {
      data: {
        action: {name: "getModelEffectiveArguments"},
        input: { missionModelId:''+mission_model_id, modelArguments: {}},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect(await response.json()).toEqual({
      success:true,
      arguments: {
        initialPlantCount: 200,
        initialDataPath: '/etc/os-release',
        initialProducer: 'Chiquita',
        initialConditions: { peel: 4, fruit: 4, flag: 'A' }}});
  });
  test('GetActivityEffectiveArgumentsBulk', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/getActivityEffectiveArgumentsBulk`, {
      data: {
        action: {name: "getActivityEffectiveArgumentsBulk"},
        input: { missionModelId: '-1', activities: [] },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such mission model');

    // Returns a 200 otherwise
    // Body contains the complete set of args for the mission model (all default in this case)
    response = await request.post(`${urls.MERLIN_URL}/getActivityEffectiveArgumentsBulk`, {
      data: {
        action: {name: "getActivityEffectiveArgumentsBulk"},
        input: {
          missionModelId:''+mission_model_id,
          activities: [
            {activityTypeName: "GrowBanana", activityArguments: {}},
            {activityTypeName: "GrowBanana", activityArguments: {quantity: 100}}
          ]
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect(await response.json()).toEqual([
      {
        typeName: 'GrowBanana',
        success: true,
        arguments: { growingDuration: 3600000000, quantity: 1 }
      },
      {
        typeName: 'GrowBanana',
        success: true,
        arguments: { growingDuration: 3600000000, quantity: 100 }
      }]);
  });
  test('AddExternalDataset', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/addExternalDataset`, {
      data: {
        action: {name: "addExternalDataset"},
        input: {
          planId: -1,
          datasetStart:'2021-001T06:00:00.000',
          profileSet: {'/my_boolean':{schema:{type:'boolean'},segments:[{duration:3600000000,dynamics:true}],type:'discrete'}},
          simulationDatasetId:null
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such plan');

    // Returns a 201 otherwise
    response = await request.post(`${urls.MERLIN_URL}/addExternalDataset`, {
      data: {
        action: {name: "addExternalDataset"},
        input: {
          planId: plan_id,
          datasetStart:'2021-001T06:00:00.000',
          profileSet: {'/my_boolean':{schema:{type:'boolean'},segments:[{duration:3600000000,dynamics:true}],type:'discrete'}},
          simulationDatasetId: null
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(201);
    expect((await response.json()).datasetId).toBeDefined();
  });
  test('ExtendExternalDataset', async ({request}) => {
    // Returns a 404 if the MissionModelId is invalid
    // message is "no such mission model"
    let response = await request.post(`${urls.MERLIN_URL}/extendExternalDataset`, {
      data: {
        action: {name: "extendExternalDataset"},
        input: {
          datasetId:-1,
          profileSet: {'/my_boolean':{schema:{type:'boolean'},segments:[{duration:3600000000,dynamics:true}],type:'discrete'}}
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such plan dataset');

    // Setup
    const datasetInput : ExternalDatasetInsertInput = {
      plan_id: plan_id,
      profile_set: {'/my_boolean':{schema:{type:'boolean'},segments:[{duration:3600000000,dynamics:true}],type:'discrete'}},
      dataset_start: '2021-001T06:00:00.000'
    }
    const dataset_id = await req.insertExternalDataset(request, datasetInput);

    // Returns a 200 if the ID is valid
    response = await request.post(`${urls.MERLIN_URL}/extendExternalDataset`, {
      data: {
        action: {name: "extendExternalDataset"},
        input: {
          datasetId:dataset_id,
          profileSet: datasetInput.profile_set
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json())).toEqual({datasetId: dataset_id});
  });
  test('ConstraintsDslTypescript', async ({request}) => {
    // Returns a 200 with a failure status if the MissionModelId is invalid
    // reason is "No mission model exists with id `-1`"
    let response = await request.post(`${urls.MERLIN_URL}/constraintsDslTypescript`, {
      data: {
        action: {name: "constraintsDslTypescript"},
        input: {
          missionModelId:'-1',
          planId: null,
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json())).toEqual({ status: 'failure', reason: 'No mission model exists with id `-1`' });

    // TODO: Uncomment this test and update the below comment once this behavior has been fixed
    // Expectation: According to `GenerateConstraintsLibAction::run`, this request should fail
    // with reason = 'No plan exists with id `-1`'.
    // However, PostgresPlanRepository's implementation of `getExternalResourceSchemas` doesn't throw NoSuchPlanException,
    // it returns an empty list if planId doesn't exist
    /*
    response = await request.post(`${urls.MERLIN_URL}/constraintsDslTypescript`, {
      data: {
        action: {name: "constraintsDslTypescript"},
        input: {
          missionModelId:''+mission_model_id,
          planId: -1,
        },
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect((await response.json())).toEqual({ status: 'failure', reason: 'No plan exists with id `-1`'});
    */

    // Returns a 200 with a success status if the ID is valid
    response = await request.post(`${urls.MERLIN_URL}/constraintsDslTypescript`, {
      data: {
        action: {name: "constraintsDslTypescript"},
        input: {
          missionModelId:''+mission_model_id,
          planId: null,
        },
        request_query: "",
        session_variables: admin.session}});
    let respBody = await response.json();
    expect(response.status()).toEqual(200);
    expect(respBody.status).toEqual('success');
    expect(respBody.typescriptFiles).not.toBeNull();
    expect(respBody.typescriptFiles.length).toBeGreaterThan(0);
    respBody.typescriptFiles.forEach(
        file => {
          expect(file.filePath).not.toBeNull();
          expect(file.content).not.toBeNull();
          expect(file.content.length).toBeGreaterThan(0);
        });
  });
});

test.describe.serial('Scheduler Bindings', () => {
  let mission_model_id: number;
  let plan_id: number;
  let scheduling_spec_id: number;
  let admin: User = {
    username: "Admin_User",
    default_role: "aerie_admin",
    allowed_roles: ["aerie_admin"],
    session: {'x-hasura-role': 'aerie_admin', 'x-hasura-user-id': 'Admin_User'}
  };
  let nonOwner: User = {
    username: "not_owner",
    default_role: "user",
    allowed_roles: ["user"],
    session: {'x-hasura-role': 'user', 'x-hasura-user-id': 'not_owner'}
  };

  test.beforeAll(async ({ request }) => {
    // Insert the users
    await req.createUser(request, admin);
    await req.createUser(request, nonOwner);

    // Insert the Mission Model
    let rd = Math.random()*100000;
    let jar_id = await req.uploadJarFile(request);
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: rd + "",
    };
    mission_model_id = await req.createMissionModel(request, model);

    // Insert the Plan
    const plan_start_timestamp = "2023-01-01T00:00:00+00:00";
    const plan_end_timestamp = "2023-01-02T00:00:00+00:00";

    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : 'test_plan_' + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input, admin.session);

    // Insert the Scheduling Spec
    const schedulingSpecification : SchedulingSpecInsertInput = {
      horizon_end: plan_end_timestamp,
      horizon_start: plan_start_timestamp,
      plan_id : plan_id,
      plan_revision : await req.getPlanRevision(request, plan_id),
      simulation_arguments : {},
      analysis_only: true
    }
    scheduling_spec_id = await req.insertSchedulingSpecification(request, schedulingSpecification);
  });
  test.afterAll(async ({ request }) => {
    // Remove Model and Plan
    await req.deleteMissionModel(request, mission_model_id);
    await req.deletePlan(request, plan_id);

    // Remove Users
    await req.deleteUser(request, admin.username);
    await req.deleteUser(request, nonOwner.username);
  });

  test('Schedule', async ({request}) => {
    // Returns a 404 if the SpecId is invalid
    // message is "no such scheduling specification"
    let response = await request.post(`${urls.SCHEDULER_URL}/schedule`, {
      data: {
        action: {name: "schedule"},
        input: {specificationId: -1},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(404);
    expect((await response.json()).message).toEqual('no such scheduling specification')

    // Returns a 403 if the user isn't authorized to schedule
    response = await request.post(`${urls.SCHEDULER_URL}/schedule`, {
      data: {
        action: {name: "schedule"},
        input: {specificationId: scheduling_spec_id},
        request_query: "",
        session_variables: nonOwner.session}});
    expect(response.status()).toEqual(403);
    expect((await response.json()).message).toEqual(
        "User '"+nonOwner.username+"' with role 'user' cannot perform 'schedule' because they are not " +
        "a 'PLAN_OWNER_COLLABORATOR' for plan with id '"+plan_id+"'");

    // Returns a 200 if the ID is valid
    response = await request.post(`${urls.SCHEDULER_URL}/schedule`, {
      data: {
        action: {name: "schedule"},
        input: {specificationId: scheduling_spec_id},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200)
  });
  test('SchedulingDslTypescript', async ({request}) => {
    // Returns a 200 with a failure status if the MissionModelId is invalid
    // reason is "No mission model exists with id `MissionModelId[id=-1]`"
    let response = await request.post(`${urls.SCHEDULER_URL}/schedulingDslTypescript`, {
      data: {
        action: {name: "schedulingDslTypescript"},
        input: {missionModelId: -1},
        request_query: "",
        session_variables: admin.session}});
    expect(response.status()).toEqual(200);
    expect(await response.json()).toEqual({
      status: 'failure',
      reason: 'No mission model exists with id `MissionModelId[id=-1]`'
    });

    // Returns a 200 with a success status if the ID is valid
    response = await request.post(`${urls.SCHEDULER_URL}/schedulingDslTypescript`, {
      data: {
        action: {name: "schedulingDslTypescript"},
        input: {missionModelId: mission_model_id},
        request_query: "",
        session_variables: admin.session}});
    let respBody = await response.json();
    expect(response.status()).toEqual(200);
    expect(respBody.status).toEqual('success');
    expect(respBody.typescriptFiles).not.toBeNull();
    expect(respBody.typescriptFiles.length).toBeGreaterThan(0);
    respBody.typescriptFiles.forEach(
        file => {
          expect(file.filePath).not.toBeNull();
          expect(file.content).not.toBeNull();
          expect(file.content.length).toBeGreaterThan(0);
        });
  });
});
