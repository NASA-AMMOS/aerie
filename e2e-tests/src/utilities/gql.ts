/**
 * GraphQL Query and Mutation strings.
 */
const gql = {
  CREATE_MISSION_MODEL: `#graphql
    mutation CreateMissionModel($model: mission_model_insert_input!) {
      insert_mission_model_one(object: $model) {
        id
      }
    }
  `,

  DELETE_MISSION_MODEL: `#graphql
    mutation DeleteModel($id: Int!) {
      delete_mission_model_by_pk(id: $id) {
        id
      }
    }
  `,

  SCHEDULE: `#graphql
    query Schedule($specificationId: Int!) {
      schedule(specificationId: $specificationId){
        reason
        status
        analysisId
      }
    }
  `,

  CREATE_PLAN: `#graphql
    mutation CreatePlan($plan: plan_insert_input!) {
      insert_plan_one(object: $plan) {
        id
        revision
      }
    }
  `,
  CREATE_SIMULATION: `#graphql
    mutation CreateSimulation($simulation: simulation_insert_input!) {
      insert_simulation_one(object: $simulation) {
        id
      }
    }
  `,

  CREATE_ACTIVITY_DIRECTIVE: `#graphql
    mutation CreateActivityDirective($activityDirectiveInsertInput: activity_directive_insert_input!) {
      createActivityDirective: insert_activity_directive_one(object: $activityDirectiveInsertInput) {
        id
      }
    }
  `,

  CREATE_SCHEDULING_GOAL: `#graphql
    mutation CreateSchedulingGoal($goal: scheduling_goal_insert_input!) {
      insert_scheduling_goal_one(object: $goal) {
        author
        created_date
        definition
        description
        id
        last_modified_by
        model_id
        modified_date
        name
        revision
      }
    }
  `,

  TRIGGER_SCHEDULING: `#graphql
  query TriggerSchedulingRun($spec_id: Int!) {
    schedule(specificationId: $spec_id){
      status
      reason
    }
  }`,

  INSERT_SCHEDULING_SPECIFICATION: `#graphql
  mutation MakeSchedulingSpec($scheduling_spec: scheduling_specification_insert_input!) {
  insert_scheduling_specification_one(object: $scheduling_spec) {
    id
  }
  }`,

  GET_PLAN_REVISION: `#graphql
    query GetPlanRevision($id: Int!) {
      plan: plan_by_pk(id: $id) {
        revision
      }
    }
  `,

  CREATE_SCHEDULING_SPEC_GOAL: `#graphql
    mutation CreateSchedulingSpecGoal($spec_goal: scheduling_specification_goals_insert_input!) {
      insert_scheduling_specification_goals_one(object: $spec_goal) {
        goal_id
        priority
        specification_id
      }
    }
  `,

  GET_SCHEDULING_DSL_TYPESCRIPT: `#graphql
    query GetSchedulingDslTypeScript($missionModelId: Int!) {
      schedulingDslTypescript(missionModelId: $missionModelId) {
        reason
        status
        typescriptFiles {
          filePath
          content
        }
      }
    }
  `,

  DELETE_PLAN: `#graphql
    mutation DeletePlan($id: Int!) {
      deletePlan: delete_plan_by_pk(id: $id) {
        id
      }
      deleteSchedulingSpec: delete_scheduling_specification(where: { plan_id: { _eq: $id } }) {
        returning {
          id
        }
      }
      deleteSimulation: delete_simulation(where: { plan_id: { _eq: $id } }) {
        returning {
          id
        }
      }
    }
  `,

  GET_PLAN: `#graphql
    query GetPlan($id: Int!) {
      plan: plan_by_pk(id: $id) {
        activity_directives {
          arguments
          id
          startOffset: start_offset
          type
        }
        constraints: conditions {
          definition
          description
          id
          model_id
          name
          plan_id
          summary
        }
        duration
        id
        model: mission_model {
          activityTypes: activity_types {
            name
            parameters
          }
          constraints: conditions {
            definition
            description
            id
            model_id
            name
            plan_id
            summary
          }
          id
          parameters {
            parameters
          }
        }
        name
        revision
        scheduling_specifications {
          id
        }
        simulations {
          arguments
          id
          template: simulation_template {
            arguments
            description
            id
          }
        }
        startTime: start_time
      }
    }
  `,

  ADD_EXTERNAL_DATASET: `#graphql
    mutation addExternalDataset($plan_id: Int!, $dataset_start: String!, $profile_set: ProfileSet!) {
      addExternalDataset(
        planId: $plan_id
        datasetStart: $dataset_start
        profileSet: $profile_set
      ) {
        datasetId
      }
    }
  `,

  GET_EXTERNAL_DATASET: `#graphql
    query getExtProfile($plan_id: Int!, $dataset_id: Int!) {
      plan_dataset_by_pk(plan_id:$plan_id, dataset_id:$dataset_id) {
        offset_from_plan_start
        dataset {
          profiles(distinct_on:[]) {
            profile_segments(distinct_on: []) {
              start_offset
              dynamics
            }
          }
        }
      }
    }
  `,

  DELETE_EXTERNAL_DATASET: `#graphql
  mutation deleteExtProfile($plan_id: Int!, $dataset_id: Int!) {
    delete_plan_dataset_by_pk(plan_id:$plan_id, dataset_id:$dataset_id) {
      dataset_id
    }
  }
  `
};

export default gql;
