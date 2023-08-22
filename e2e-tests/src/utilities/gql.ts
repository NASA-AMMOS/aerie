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

  GET_RESOURCE_TYPES: `#graphql
    query GetResourceTypes($missionModelId: Int!) {
      resource_type(where: {model_id: {_eq: $missionModelId}}, order_by: {name: asc}) {
        name
        schema
      }
    }
  `,

  GET_ACTIVITY_TYPES: `#graphql
    query GetActivityTypes($missionModelId: Int!) {
      activity_type(where: {model_id: {_eq: $missionModelId}}, order_by: {name: asc}) {
        name
        parameters
      }
    }
  `,

  GET_TOPIC_EVENTS: `#graphql
  query GetTopicsEvents($datasetId: Int!) {
    topic(where: {dataset_id: {_eq: $datasetId}}) {
      name
      value_schema
      events {
        causal_time
        real_time
        topic_index
        transaction_index
        value
      }
    }
  }
  `,

  GET_PROFILES: `#graphql
    query GetProfiles($datasetId: Int!){
      profile(where: {dataset_id: {_eq: $datasetId}}) {
        name
        profile_segments {
          dynamics
          is_gap
          start_offset
        }
    }
  }
  `,

  SCHEDULE: `#graphql
    query Schedule($specificationId: Int!) {
      schedule(specificationId: $specificationId){
        reason
        status
        analysisId
        datasetId
      }
    }
  `,

  SIMULATE: `#graphql
  query Simulate($plan_id: Int!) {
    simulate(planId: $plan_id){
      status
      reason
      simulationDatasetId
    }
  }`,

  CREATE_PLAN: `#graphql
    mutation CreatePlan($plan: plan_insert_input!) {
      insert_plan_one(object: $plan) {
        id
        revision
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

  DELETE_ACTIVITY_DIRECTIVE: `#graphql
    mutation DeleteActivityDirective($id: Int!, $plan_id: Int!) {
      delete_activity_directive_by_pk(id: $id, plan_id: $plan_id) {
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

  GET_SIMULATION_DATASET: `#graphql
  query GetSimulationDataset($id: Int!) {
    simulationDataset: simulation_dataset_by_pk(id: $id) {
      canceled
      simulation_start_time
      simulation_end_time
      simulated_activities {
        activity_directive { id }
        duration
        start_time
        start_offset
      }
    }
  }
  `,

  GET_SIMULATION_DATASET_BY_DATASET_ID: `#graphql
    query GetSimulationDataset($id: Int!) {
      simulation_dataset(where: {dataset_id: {_eq: $id}}) {
        canceled
        simulation_start_time
        simulation_end_time
        simulated_activities {
          activity_directive { id }
          duration
          start_time
          start_offset
        }
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
        constraints {
          definition
          description
          id
          model_id
          name
          plan_id
        }
        duration
        id
        model: mission_model {
          activityTypes: activity_types {
            name
            parameters
          }
          constraints {
            definition
            description
            id
            model_id
            name
            plan_id
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
          simulation_start_time
          simulation_end_time
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

  INSERT_SPAN:`#graphql
  mutation InsertSpan(
    $parentId: Int!,
    $duration: interval,
    $datasetId: Int!,
    $type: String,
    $startOffset: interval,
    $attributes: jsonb
  ){
  insert_span_one(object: {parent_id: $parentId, duration: $duration, dataset_id: $datasetId, type: $type, start_offset: $startOffset, attributes: $attributes}) {
    id
  }
}
`,

INSERT_SIMULATION_DATASET:`#graphql
    mutation InsertSimulationDataset($simulationDatasetInsertInput:simulation_dataset_insert_input!
      ){
      insert_simulation_dataset_one(object: $simulationDatasetInsertInput) {
        dataset_id
      }
    }
  `,

  INSERT_PROFILE: `#graphql
  mutation insertProfile($datasetId: Int!, $duration:interval, $name:String, $type:jsonb){
    insert_profile_one(object: {dataset_id: $datasetId, duration: $duration, name: $name, type: $type}) {
      id
    }
  }
  `,

  INSERT_PROFILE_SEGMENT:`#graphql
  mutation insertProfileSegment($datasetId: Int!, $dynamics:jsonb, $isGap: Boolean, $profileId:Int!, $startOffset:interval){
    insert_profile_segment_one(object: {dataset_id: $datasetId, dynamics: $dynamics, is_gap: $isGap, profile_id: $profileId, start_offset: $startOffset}){
      dataset_id
    }
  }
  `,

  INSERT_SIMULATION_TEMPLATE: `#graphql
    mutation CreateSimulationTemplate($simulationTemplateInsertInput: simulation_template_insert_input!) {
      insert_simulation_template_one(object: $simulationTemplateInsertInput) {
        id
      }
    }
  `,

  ASSIGN_TEMPLATE_TO_SIMULATION: `#graphql
    mutation AssignTemplateToSimulation($simulation_id: Int!, $simulation_template_id: Int!) {
      update_simulation_by_pk(pk_columns: {id: $simulation_id}, _set: {simulation_template_id: $simulation_template_id}) {
        simulation_template_id
      }
    }
  `,

  GET_SIMULATION_ID: `#graphql
    query getSimulationId($plan_id: Int!) {
      simulation: simulation(where: {plan_id: {_eq: $plan_id}}) {
        id
      }
    }
  `,

  UPDATE_SIMULATION_BOUNDS: `#graphql
    mutation updateSimulationBounds($plan_id: Int!, $simulation_start_time: timestamptz!, $simulation_end_time: timestamptz!) {
      update_simulation(where: {plan_id: {_eq: $plan_id}},
      _set: {
        simulation_start_time: $simulation_start_time,
        simulation_end_time: $simulation_end_time}) {
        affected_rows
       }
    }
  `,

  ADD_EXTERNAL_DATASET: `#graphql
    mutation addExternalDataset($plan_id: Int!, $simulation_dataset_id: Int, $dataset_start: String!, $profile_set: ProfileSet!) {
      addExternalDataset(
        planId: $plan_id
        simulationDatasetId: $simulation_dataset_id
        datasetStart: $dataset_start
        profileSet: $profile_set
      ) {
        datasetId
      }
    }
  `,

  EXTEND_EXTERNAL_DATASET: `#graphql
  mutation extendExternalDataset($dataset_id: Int!, $profile_set: ProfileSet!) {
    extendExternalDataset(
      datasetId: $dataset_id
      profileSet: $profile_set
    ) {
      datasetId
    }
  }
  `,

  GET_EXTERNAL_DATASET: `#graphql
    query getExtProfile($plan_id: Int!, $dataset_id: Int!) {
      plan_dataset_by_pk(plan_id:$plan_id, dataset_id:$dataset_id) {
        simulation_dataset_id
        offset_from_plan_start
        dataset {
          profiles(distinct_on:[], order_by: { name: asc }) {
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
  `,

  INSERT_CONSTRAINT: `#graphql
    mutation insertConstraint($constraint: constraint_insert_input!) {
      insert_constraint_one(object: $constraint) {
        id
      }
    }
  `,

  UPDATE_CONSTRAINT: `#graphql
    mutation updateConstraint($constraintId: Int!, $constraintDefinition: String!) {
      update_constraint(where: {id: {_eq: $constraintId}}, _set: {definition: $constraintDefinition}) {
        returning {
          definition
        }
      }
    }
  `,

  CHECK_CONSTRAINTS: `#graphql
    query checkConstraints($planId: Int!, $simulationDatasetId: Int) {
      constraintViolations(planId: $planId, simulationDatasetId: $simulationDatasetId) {
        constraintId
        constraintName
        type
        resourceIds
        violations {
          activityInstanceIds
          windows {
            start
            end
          }
        }
        gaps {
          start
          end
        }
      }
    }
  `,

  DELETE_CONSTRAINT: `#graphql
    mutation DeleteConstraint($id: Int!) {
      delete_constraint_by_pk(id: $id) {
        id
      }
    }
  `,

  GET_CONSTRAINT_RUNS: `#graphql
    query getConstraintRuns($simulationDatasetId: Int!) {
      constraint_run(where: {simulation_dataset_id: {_eq: $simulationDatasetId}}) {
        constraint_definition
        constraint_id
        simulation_dataset_id
        definition_outdated
        violations
      }
    }
  `,

  GET_EFFECTIVE_ACTIVITY_ARGUMENTS: `#graphql
    query GetEffectiveActivityArguments($modelId: ID!, $activityTypeName: String!, $activityArguments: ActivityArguments!) {
      getActivityEffectiveArguments(
        missionModelId: $modelId,
        activityTypeName: $activityTypeName,
        activityArguments: $activityArguments
      ) {
        arguments
        errors
        success
      }
    }
  `,

  GET_EFFECTIVE_ACTIVITY_ARGUMENTS_BULK: `#graphql
    query GetEffectiveActivityArgumentsBulk($modelId: ID!, $activities: [EffectiveArgumentsInput!]!) {
      getActivityEffectiveArgumentsBulk(
        missionModelId: $modelId,
        activities: $activities
      ) {
        typeName
        arguments
        errors
        success
      }
    }
  `,

  CREATE_USER: `#graphql
    mutation createUser($user: users_insert_input!, $allowed_roles: [users_allowed_roles_insert_input!]!) {
      insert_users_one(object: $user) {
        default_role
        username
      }
      insert_users_allowed_roles(objects: $allowed_roles) {
        returning {
          allowed_role
          username
        }
      }
    }
  `,

  DELETE_USER: `#graphql
    mutation deleteUser($username: String!) {
      delete_users_by_pk(username: $username) {
        username
        default_role
      }
    }
  `,

  ADD_PLAN_COLLABORATOR: `#graphql
    mutation addPlanCollaborator($collaborator: plan_collaborators_insert_input!) {
      insert_plan_collaborators_one(object: $collaborator) {
        collaborator
        plan_id
      }
    }
  `,

  GET_ROLE_ACTION_PERMISSIONS: `#graphl
    query getRolePermissions($role: user_roles_enum!){
      permissions: user_role_permission_by_pk(role: $role) {
        action_permissions
      }
    }
  `,

  UPDATE_ROLE_ACTION_PERMISSIONS: `#graphl
    mutation updateRolePermissions($role: user_roles_enum!, $action_permissions: jsonb!) {
      permissions: update_user_role_permission_by_pk(
        pk_columns: {role: $role},
        _set: {action_permissions: $action_permissions})
      {
        action_permissions
      }
    }
  `
};

export default gql;
