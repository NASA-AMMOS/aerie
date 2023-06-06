import { expect, test } from "@playwright/test";
import req, { awaitSimulation } from "../utilities/requests.js";
import time from "../utilities/time.js";

let constraint_id: number;
let jar_id: number;
let mission_model_id: number;
let plan_id: number;
let violation: ConstraintViolation;
let activity_id: number;
const first_constraint_name = "fruit_equal_peel"
const second_constraint_name = "no_controllable_duration"
const long_duration = 35 * 24* 60 * 60 * 1000 * 1000 // 35 days in microseconds
const rd = Math.random() * 100;
const plan_start_timestamp = "2021-001T00:00:00.000";
const plan_end_timestamp = "2021-050T12:00:00.000";
const activity_offset_hours = 1;

// Tests in Initialize don't involve constraints (and therefore shouldn't fail),
// they simply set up Aerie with a model, plan, and activities for further testing
test.describe.serial("Initialize", async () => {
  test("Upload jar and create mission model", async ({ request }) => {
    jar_id = await req.uploadJarFile(request);

    const model: MissionModelInsertInput = {
      jar_id,
      mission: "aerie_e2e_tests" + rd,
      name: "Banananation (e2e tests)"+rd,
      version: "0.0.0"+ rd,
    };
    mission_model_id = await req.createMissionModel(request, model);

    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual("number");

    // delay 2000ms for model generation
    await time.delay(2000);
  });

  test("Create Plan", async ({ request }) => {
    const plan_input : CreatePlanInput = {
      model_id : mission_model_id,
      name : "test_plan" + rd,
      start_time : plan_start_timestamp,
      duration : time.getIntervalFromDoyRange(plan_start_timestamp, plan_end_timestamp)
    };
    plan_id = await req.createPlan(request, plan_input);

    expect(plan_id).not.toBeNull();
    expect(plan_id).toBeDefined();
    expect(typeof plan_id).toEqual("number");
  });

  test("Add Activity to Plan", async ({ request }) => {
    const activityToInsert : ActivityInsertInput = {
      arguments : {
        "biteSize": 1
      },
      plan_id: plan_id,
      type : "BiteBanana",
      start_offset : activity_offset_hours + "h",
    };
    activity_id = await req.insertActivity(request, activityToInsert);

    expect(activity_id).not.toBeNull();
    expect(activity_id).toBeDefined();
  });

});

test.describe("Constraints", () => {
  test("Add Constraint to Plan", async ({ request }) => {
    const constraint: ConstraintInsertInput = {
      name: first_constraint_name,
      definition: "export default (): Constraint => Real.Resource(\"/fruit\").equal(Real.Resource(\"/peel\"))",
      description: "",
      model_id: null,
      plan_id,
    };
    constraint_id = await req.insertConstraint(request, constraint);

    expect(constraint_id).not.toBeNull();
    expect(constraint_id).toBeDefined();
  });

  test("Check there are no violations yet", async ({ request }) => {
    const violations: ConstraintViolation[] = await req.checkConstraints(request, plan_id);

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(0);
  });

  test("Run simulation", async ({ request }) => {
    const resp: SimulationResponse = await awaitSimulation(request, plan_id);

    expect(resp.status).toEqual("complete");
  });

  test("Check there is one violation", async ({ request }) => {
    const violations: ConstraintViolation[] = await req.checkConstraints(request, plan_id);

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(1);

    violation = violations[0];

    expect(violation).not.toBeNull();
    expect(violation).toBeDefined();
  });

  test("Check the violation is the expected one", async () => {
    expect(violation.constraintName).toEqual(first_constraint_name)
    expect(violation.constraintId).toEqual(constraint_id)
    expect(violation.associations.resourceIds).toHaveLength(2);
    expect(violation.associations.resourceIds).toContain("/fruit");
    expect(violation.associations.resourceIds).toContain("/peel");
  });

  test("Check violation starts and ends as expected", async () => {
    const plan_start_unix = time.getUnixEpochTime(plan_start_timestamp);
    const plan_end_unix = time.getUnixEpochTime(plan_end_timestamp);
    const plan_duration_micro = (plan_end_unix - plan_start_unix) * 1000;
    const activity_offset_micro = activity_offset_hours * 60 * 60 * 1000 * 1000;

    expect(violation.windows[0].start).toEqual(activity_offset_micro);
    expect(violation.windows[0].end).toEqual(plan_duration_micro);
  });

  test("Check delete violating activity", async ({ request }) => {
    const id = await req.deleteActivity(request, plan_id, activity_id);
    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(activity_id);
  });

  test("Run new simulation", async ({ request }) => {
    const resp: SimulationResponse = await awaitSimulation(request, plan_id);

    expect(resp.status).toEqual("complete");
  });

  test("Check there are no violations anymore", async ({ request }) => {
    const violations: ConstraintViolation[] = await req.checkConstraints(request, plan_id);

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(0);
  });

  /*
  Test that an activity with a duration longer than one month is written to and read back from the database successfully
  by Aerie's simulation and constraints checking components respectively. The driving concern here is that Aerie needs
  to interpret span durations as microseconds; if the simulation results were to be written using the postgres
  interval's "months" field, constraints checking would fail to load these values back from the database.
   */

  test("Add No ControllableDurationActivity Constraint to Plan", async ({ request }) => {
    const constraint: ConstraintInsertInput = {
      name: second_constraint_name,
      definition: "export default (): Constraint => Windows.During(ActivityType.ControllableDurationActivity).not()",
      description: "",
      model_id: null,
      plan_id,
    };
    constraint_id = await req.insertConstraint(request, constraint);

    expect(constraint_id).not.toBeNull();
    expect(constraint_id).toBeDefined();
  });

  test("Check insert long violating activity", async ({ request }) => {
    activity_id = await req.insertActivity(request, {
      arguments : {
        "duration": long_duration
      },
      plan_id: plan_id,
      type : "ControllableDurationActivity",
      start_offset : "0h",
    });

    expect(activity_id).not.toBeNull();
    expect(activity_id).toBeDefined();
  });

  test("Run simulation of long activity", async ({ request }) => {
    const resp: SimulationResponse = await awaitSimulation(request, plan_id);

    expect(resp.status).toEqual("complete");
  });

  test("Check there is one violation again", async ({ request }) => {
    const violations: ConstraintViolation[] = await req.checkConstraints(request, plan_id);

    expect(violations).not.toBeNull();
    expect(violations).toBeDefined();
    expect(violations).toHaveLength(1);

    violation = violations[0];

    expect(violation).not.toBeNull();
    expect(violation).toBeDefined();
  });

  test("Check the violation window duration is correct", async () => {
    expect(violation.constraintName).toEqual(second_constraint_name);
    expect(violation.windows).toHaveLength(1);
    expect(violation.windows[0]).toEqual({
      start: 0,
      end: long_duration
    });
  });
});

test.describe("Clean Up", async () => {
  test("Check constraint is deleted", async ({ request }) => {
    const id = await req.deleteConstraint(request, constraint_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(constraint_id);
  });

  test("Check plan is deleted", async ({ request }) => {
    const id = await req.deletePlan(request, plan_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(plan_id);
  });

  test("Check model is deleted", async ({ request }) => {
    const id = await req.deleteMissionModel(request, mission_model_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(mission_model_id);
  });

});
