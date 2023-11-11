package gov.nasa.jpl.aerie.e2e.utils;

public enum GQL {
  ADD_EXTERNAL_DATASET("""
    mutation addExternalDataset($plan_id: Int!, $simulation_dataset_id: Int, $dataset_start: String!, $profile_set: ProfileSet!) {
      addExternalDataset(
        planId: $plan_id
        simulationDatasetId: $simulation_dataset_id
        datasetStart: $dataset_start
        profileSet: $profile_set
      ) {
        datasetId
      }
    }"""),
  ADD_PLAN_COLLABORATOR("""
    mutation addPlanCollaborator($collaborator: plan_collaborators_insert_input!) {
      insert_plan_collaborators_one(object: $collaborator) {
        collaborator
        plan_id
      }
    }"""),
  ASSIGN_TEMPLATE_TO_SIMULATION("""
    mutation AssignTemplateToSimulation($simulation_id: Int!, $simulation_template_id: Int!) {
      update_simulation_by_pk(pk_columns: {id: $simulation_id}, _set: {simulation_template_id: $simulation_template_id}) {
        simulation_template_id
      }
    }"""),
  CHECK_CONSTRAINTS("""
    query checkConstraints($planId: Int!, $simulationDatasetId: Int) {
      constraintViolations(planId: $planId, simulationDatasetId: $simulationDatasetId) {
        success
        results {
          constraintId
          constraintName
          resourceIds
          type
          gaps {
            end
            start
          }
          violations {
            activityInstanceIds
            windows {
              end
              start
            }
          }
        }
        errors {
          message
          stack
          location {
            column
            line
          }
        }
      }
    }"""),
  CREATE_ACTIVITY_DIRECTIVE("""
    mutation CreateActivityDirective($activityDirectiveInsertInput: activity_directive_insert_input!) {
      createActivityDirective: insert_activity_directive_one(object: $activityDirectiveInsertInput) {
        id
      }
    }"""),
  CREATE_MISSION_MODEL("""
    mutation CreateMissionModel($model: mission_model_insert_input!) {
      insert_mission_model_one(object: $model) {
        id
      }
    }"""),
  CREATE_PLAN("""
    mutation CreatePlan($plan: plan_insert_input!) {
      insert_plan_one(object: $plan) {
        id
        revision
      }
    }"""),
  CREATE_SCHEDULING_GOAL("""
    mutation CreateSchedulingGoal($goal: scheduling_goal_insert_input!) {
      goal: insert_scheduling_goal_one(object: $goal) {
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
    }"""),
  CREATE_SCHEDULING_SPEC_GOAL("""
    mutation CreateSchedulingSpecGoal($spec_goal: scheduling_specification_goals_insert_input!) {
      insert_scheduling_specification_goals_one(object: $spec_goal) {
        goal_id
        priority
        specification_id
      }
    }"""),
  CREATE_USER("""
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
    }"""),
  DELETE_ACTIVITY_DIRECTIVE("""
    mutation DeleteActivityDirective($id: Int!, $plan_id: Int!) {
      delete_activity_directive_by_pk(id: $id, plan_id: $plan_id) {
        id
      }
    }"""),
  DELETE_CONSTRAINT("""
    mutation DeleteConstraint($id: Int!) {
      delete_constraint_by_pk(id: $id) {
        id
      }
    }"""),
  DELETE_EXTERNAL_DATASET("""
    mutation deleteExtProfile($plan_id: Int!, $dataset_id: Int!) {
      delete_plan_dataset_by_pk(plan_id:$plan_id, dataset_id:$dataset_id) {
        dataset_id
      }
    }"""),
  DELETE_MISSION_MODEL("""
    mutation DeleteModel($id: Int!) {
      delete_mission_model_by_pk(id: $id) {
        id
      }
    }"""),
  DELETE_PLAN("""
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
    }"""),
  DELETE_SCHEDULING_GOAL("""
    mutation DeleteSchedulingGoal($goalId: Int!) {
      delete_scheduling_goal_by_pk(id: $goalId) {
        name
        definition
      }
    }"""),
  DELETE_SIMULATION_PRESET("""
    mutation DeleteSimPreset($templateId: Int!) {
      delete_simulation_template_by_pk(id: $templateId) {
        id
      }
    }"""),
  DELETE_USER("""
    mutation deleteUser($username: String!) {
      delete_users_by_pk(username: $username) {
        username
        default_role
      }
    }"""),
  EXTEND_EXTERNAL_DATASET("""
    mutation extendExternalDataset($dataset_id: Int!, $profile_set: ProfileSet!) {
      extendExternalDataset(
        datasetId: $dataset_id
        profileSet: $profile_set
      ) {
        datasetId
      }
    }"""),
  GET_ACTIVITY_TYPES("""
    query GetActivityTypes($missionModelId: Int!) {
      activity_type(where: {model_id: {_eq: $missionModelId}}, order_by: {name: asc}) {
        name
        parameters
        computed_attributes_value_schema
      }
    }"""),
  GET_CONSTRAINT_RUNS("""
    query getConstraintRuns($simulationDatasetId: Int!) {
      constraint_run(where: {simulation_dataset_id: {_eq: $simulationDatasetId}}) {
        constraint_definition
        constraint_id
        simulation_dataset_id
        definition_outdated
        results
      }
    }"""),
  GET_EFFECTIVE_ACTIVITY_ARGUMENTS_BULK("""
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
    }"""),
  GET_ACTIVITY_VALIDATIONS("""
    query GetActivityValidations($planId: Int!) {
      activity_directive_validations(where: {plan_id: {_eq: $planId}}) {
        status
        validations
        plan_id
        last_modified_arguments_at
        directive_id
      }
    }"""),
  GET_EFFECTIVE_MODEL_ARGUMENTS("""
    query GetEffectiveModelArguments($modelId: ID!, $modelArgs: ModelArguments!) {
      getModelEffectiveArguments(missionModelId: $modelId, modelArguments: $modelArgs) {
        arguments
        errors
        success
      }
    }"""),
  GET_EXTERNAL_DATASET("""
    query getExternalDataset($plan_id: Int!, $dataset_id: Int!) {
      externalDataset: plan_dataset_by_pk(plan_id:$plan_id, dataset_id:$dataset_id) {
        plan_id
        dataset_id
        simulation_dataset_id
        offset_from_plan_start
        dataset {
          profiles(distinct_on:[], order_by: { name: asc }) {
            name
            profile_segments(distinct_on: []) {
              start_offset
              is_gap
              dynamics
            }
          }
        }
      }
    }"""),
  GET_PLAN("""
    query GetPlan($id: Int!) {
      plan: plan_by_pk(id: $id) {
        activity_directives {
          arguments
          id
          plan_id
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
    }"""),
  GET_PLAN_REVISION("""
    query GetPlanRevision($id: Int!) {
      plan: plan_by_pk(id: $id) {
        revision
      }
    }"""),
  GET_PROFILES("""
    query GetProfiles($datasetId: Int!){
      profile(where: {dataset_id: {_eq: $datasetId}}) {
        name
        profile_segments {
          dynamics
          is_gap
          start_offset
        }
    }
    }"""),
  GET_RESOURCE_TYPES("""
    query GetResourceTypes($missionModelId: Int!) {
      resource_type(where: {model_id: {_eq: $missionModelId}}, order_by: {name: asc}) {
        name
        schema
      }
    }"""),
  GET_ROLE_ACTION_PERMISSIONS("""
    query getRolePermissions($role: user_roles_enum!){
      permissions: user_role_permission_by_pk(role: $role) {
        action_permissions
      }
    }"""),
  GET_SCHEDULING_DSL_TYPESCRIPT("""
    query GetSchedulingDslTypeScript($missionModelId: Int!, $planId: Int) {
      schedulingDslTypescript(missionModelId: $missionModelId, planId: $planId) {
        reason
        status
        typescriptFiles {
          filePath
          content
        }
      }
    }"""),
  GET_SIMULATION_DATASET("""
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
          parent_id
          type: activity_type_name
          id
        }
      }
    }"""),
  GET_SIMULATION_DATASET_BY_DATASET_ID("""
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
          parent_id
          type: activity_type_name
          id
        }
      }
    }"""),
  GET_SIMULATION_ID("""
    query getSimulationId($plan_id: Int!) {
      simulation: simulation(where: {plan_id: {_eq: $plan_id}}) {
        id
      }
    }"""),
  GET_TOPIC_EVENTS("""
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
    }"""),
  INSERT_CONSTRAINT("""
    mutation insertConstraint($constraint: constraint_insert_input!) {
      constraint: insert_constraint_one(object: $constraint) {
        id
      }
    }"""),
  INSERT_PROFILE("""
    mutation insertProfile($datasetId: Int!, $duration:interval, $name:String, $type:jsonb){
      insert_profile_one(object: {dataset_id: $datasetId, duration: $duration, name: $name, type: $type}) {
        id
      }
    }"""),
  INSERT_PROFILE_SEGMENTS("""
    mutation insertProfileSegment($segments: [profile_segment_insert_input!]!){
      insert_profile_segment(objects: $segments){
        affected_rows
      }
    }"""),
  INSERT_SCHEDULING_SPECIFICATION("""
    mutation MakeSchedulingSpec($scheduling_spec: scheduling_specification_insert_input!) {
      scheduling_spec: insert_scheduling_specification_one(object: $scheduling_spec) {
        id
      }
    }"""),
  INSERT_SIMULATION_DATASET("""
    mutation InsertSimulationDataset($simulationDataset:simulation_dataset_insert_input!){
      simulation_dataset: insert_simulation_dataset_one(object: $simulationDataset) {
        dataset_id
      }
    }"""),
  INSERT_SIMULATION_TEMPLATE("""
    mutation CreateSimulationTemplate($simulationTemplate: simulation_template_insert_input!) {
      template: insert_simulation_template_one(object: $simulationTemplate) {
        id
      }
    }"""),
  INSERT_SPAN("""
    mutation InsertSpan($span: span_insert_input!){
      span: insert_span_one(object: $span) {
        id
      }
    }"""),
  SCHEDULE("""
    query Schedule($specificationId: Int!) {
      schedule(specificationId: $specificationId){
        reason
        status
        analysisId
        datasetId
      }
    }"""),
  SIMULATE("""
    query Simulate($plan_id: Int!) {
      simulate(planId: $plan_id){
        status
        reason
        simulationDatasetId
      }
    }"""),
  UPDATE_CONSTRAINT("""
    mutation updateConstraint($constraintId: Int!, $constraintDefinition: String!) {
      update_constraint(where: {id: {_eq: $constraintId}}, _set: {definition: $constraintDefinition}) {
        returning {
          definition
        }
      }
    }"""),
  UPDATE_ROLE_ACTION_PERMISSIONS("""
    mutation updateRolePermissions($role: user_roles_enum!, $action_permissions: jsonb!) {
      permissions: update_user_role_permission_by_pk(
        pk_columns: {role: $role},
        _set: {action_permissions: $action_permissions})
      {
        action_permissions
      }
    }"""),
  UPDATE_SCHEDULING_SPECIFICATION_PLAN_REVISION("""
    mutation updateSchedulingSpec($planId: Int!, $planRev: Int!) {
      update_scheduling_specification(
        where: {plan_id: {_eq: $planId}},
        _set: {plan_revision: $planRev})
      {
        affected_rows
      }
    }"""),
  UPDATE_SIMULATION_BOUNDS("""
    mutation updateSimulationBounds($plan_id: Int!, $simulation_start_time: timestamptz!, $simulation_end_time: timestamptz!) {
      update_simulation(where: {plan_id: {_eq: $plan_id}},
      _set: {
        simulation_start_time: $simulation_start_time,
        simulation_end_time: $simulation_end_time}) {
        affected_rows
       }
    }""");
  public final String query;
  GQL(String query) {
    this.query = query;
  }
}
