import { expect, test } from '@playwright/test';
import req from '../utilities/requests.js';
import time from '../utilities/time.js';

const eqSet = (xs, ys) =>
    xs.size === ys.size &&
    [...xs].every((x) => ys.has(x));


test.describe('Scheduling', () => {
  const rd = Math.random() * 100;
  const plan_start_timestamp = "2021-001T00:00:00.000";
  const plan_end_timestamp = "2021-001T12:00:00.000";
  let jar_id: number;
  let mission_model_id: number;
  let plan_id: number;
  let first_goal_id: number;
  let second_goal_id: number;
  let plan_revision: number;
  let specification_id: number;
  let dataset_id:number;

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

  test('Get scheduling DSL TypeScript', async ({ request }) => {
    const schedulingDslTypes = await req.getSchedulingDslTypeScript(request, mission_model_id);
    expect(schedulingDslTypes.typescriptFiles.length).toEqual(7);
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
                    interval: Temporal.Duration.from({hours:1})
                  })
                }`
        };

    first_goal_id = await req.insertSchedulingGoal(request, schedulingGoal);
    expect(first_goal_id).not.toBeNull();
    expect(first_goal_id).toBeDefined();
    expect(typeof first_goal_id).toEqual("number");
  });

  //we insert a GrowBanana activity which has a controllable duration but we do not
  //specify its duration. The scheduler will pick the effective arguments and thus its default duration
  test('Insert Activity', async ({ request }) =>{

    const activityToInsert : ActivityInsertInput =
        {
          //no arguments to ensure that the scheduler is getting effective arguments
          arguments : {},
          plan_id: plan_id,
          type : "GrowBanana",
          start_offset : "1h"
        };

    let activity_id = await req.insertActivity(request, activityToInsert);
    expect(activity_id).not.toBeNull();
    expect(activity_id).toBeDefined();
    expect(typeof activity_id).toEqual("number");
  });

  test('Create Second Scheduling goal', async ({ request }) =>{

    const schedulingGoal : SchedulingGoalInsertInput =
        {
          last_modified_by : "test",
          description: "Second test goal",
          author: "Test",
          model_id: mission_model_id,
          name: "my second scheduling goal!"+rd,
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

    second_goal_id = await req.insertSchedulingGoal(request, schedulingGoal);
    expect(second_goal_id).not.toBeNull();
    expect(second_goal_id).toBeDefined();
    expect(typeof second_goal_id).toEqual("number");
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
      simulation_arguments : {},
      analysis_only: false
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
      goal_id: first_goal_id,
      priority: priority,
      specification_id: specification_id,
    };
    const returned_spec_id = await req.createSchedulingSpecGoal(request, specGoal);
    expect(returned_spec_id).not.toBeNull();
    expect(returned_spec_id).toBeDefined();
    expect(returned_spec_id).toEqual(specification_id);
  });

  test('Create Scheduling 2nd Specification Goal', async ({ request }) => {
    const priority = 1;
    const specGoal: SchedulingSpecGoalInsertInput = {
      // @ts-ignore
      goal_id: second_goal_id,
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
    let analysisId_local: number;
    const { reason, status, analysisId } = await req.schedule(request, specification_id);
    expect(status).not.toBeNull();
    expect(status).toBeDefined();
    expect(analysisId).not.toBeNull();
    expect(analysisId).toBeDefined();
    expect(typeof analysisId).toEqual("number")
    analysisId_local = analysisId;
    const max_it = 10;
    let it = 0;
    let reason_local: string;
    while (it++ < max_it && (status == 'pending' || status == 'incomplete')) {
      const { reason, status, analysisId, datasetId } = await req.schedule(request, specification_id);
      status_local = status;
      reason_local = reason;
      expect(status).not.toBeNull();
      expect(status).toBeDefined();
      dataset_id = datasetId
      await delay(1000);
    }
    if (status_local == "failed") {
      console.error(reason_local)
      throw new Error(reason_local);
    }
    expect(status_local).toEqual("complete")
    expect(analysisId_local).toEqual(analysisId)
    expect(dataset_id).not.toBeNull();
  });

  test('Verify posting of simulation results', async ({ request }) => {
    let plan = await req.getPlan(request, plan_id)
    let directiveIds = new Set<number>()
    let startOffsetsActivityDirectives = new Set<string>()
    for(const activity of plan.activity_directives){
      directiveIds.add(activity['id'])
    }
    const activities = await req.getSimulationDatasetByDatasetId(request,dataset_id)
    expect(activities.simulated_activities.length).toEqual(plan.activity_directives.length)
    let refDirectiveIds = new Set<number>()
    for(let simulated_activity of activities.simulated_activities){
      refDirectiveIds.add(simulated_activity.activity_directive.id)
      startOffsetsActivityDirectives.add(simulated_activity.start_offset)
    }
    //all directive have their simulated activity
    expect(eqSet(refDirectiveIds,directiveIds))
    const dataset = await req.getProfiles(request, dataset_id)
    //expect one profile per resource in the banananation model
    expect(dataset.length).toEqual(7)
    for(let resource of dataset){
      if(resource.name == "/fruit" || resource.name == "/peel"){
        let startOffsetOfResource = new Set<string>()
        for(let segment of resource.profile_segments){
          startOffsetOfResource.add(segment.start_offset)
        }
        //these two resources are affected by the peel, grow and bite banana
        expect(eqSet(startOffsetOfResource, startOffsetsActivityDirectives)).toEqual(true)
      }
    }
    let topics = await req.getTopicsEvents(request, dataset_id)
    let prefixInput = "ActivityType.Input."
    let prefixOutput = "ActivityType.Output."
    let peelInputIsThere = false
    let biteInputIsThere = false
    let growInputIsThere = false
    let peelOutputIsThere = false
    let biteOutputIsThere = false
    let growOutputIsThere = false
    for(let topic of topics){
      switch (topic.name){
        case prefixInput+"BiteBanana":
          biteInputIsThere = true
          expect(topic.events.length).toEqual(1)
          break
        case prefixOutput+"BiteBanana":
          biteOutputIsThere = true
          expect(topic.events.length).toEqual(1)
          break
        case prefixInput+"GrowBanana":
          growInputIsThere = true
          expect(topic.events.length).toEqual(1)
          break
        case prefixOutput+"GrowBanana":
          growOutputIsThere = true
          expect(topic.events.length).toEqual(1)
          break
        case prefixInput+"PeelBanana":
          peelInputIsThere = true
          expect(topic.events.length).toEqual(12)
          break
        case prefixOutput+"PeelBanana":
          peelOutputIsThere = true
          expect(topic.events.length).toEqual(12)
          break
        default:
          test.fail(topic.events.length !== 0, "Unexpected topic with events: "+topic.name)
          break
      }
    }
    expect(peelInputIsThere)
    expect(growInputIsThere)
    expect(biteInputIsThere)
    expect(peelOutputIsThere)
    expect(growOutputIsThere)
    expect(biteOutputIsThere)
  });

  test('Get Plan', async ({ request }) => {
    //check number of activities
    const plan = await req.getPlan(request, plan_id)
    expect(plan).not.toBeNull();
    expect(plan).toBeDefined();
    expect(plan.id).toEqual(plan_id);
    expect(plan.activity_directives.length).toEqual(14);
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
