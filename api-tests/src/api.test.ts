import {fileFromSync} from 'fetch-blob/from.js'
import fetch, {Response} from 'node-fetch'
import {FormData} from 'formdata-polyfill/esm.min.js'
import {DateTime} from 'luxon'
import assert from "assert"

const API_URL = "http://localhost"
const GRAPHQL_API_URL = API_URL + ":8080/v1/graphql"
const FILES_API_URL = API_URL + ":9000/file"
const LOGIN_URL = API_URL + ":9000/auth/login"


interface MissionModelMetadata {
  mission: string,
  version: string,
  name: string
}

async function main() {
  let pathToAerieLanderMissionModelJar = "/Users/dailis/projects/AERIE/aerie/aerielander/build/libs/aerielander-0.10.0-SNAPSHOT.jar"
  let pathToAerieLanderRulesJar = "/Users/dailis/projects/AERIE/aerie/scheduler/src/test/resources/merlinsight-rules.jar"

  const ssoToken = await login("USERNAME", "PASSWORD")  // TODO find a way that doesn't require hard-coding username and password

  await testSchedulerWorkflow(pathToAerieLanderMissionModelJar, pathToAerieLanderRulesJar, ssoToken)
}

/**
 * This test is considered successful if it completes without throwing an Error
 */
async function testSchedulerWorkflow(pathToAerieLanderMissionModelJar: string, pathToAerieLanderRulesJar: string, ssoToken: string) {
  await uploadFile(pathToAerieLanderRulesJar, "scheduler_rules.jar", ssoToken)
  let missionModelMetadata = {mission: "aerielander", name: "aerielander", version: "1"}
  const missionModelId = await ensureMissionModelHasBeenUploaded(missionModelMetadata, ssoToken, pathToAerieLanderMissionModelJar)

  const planStartTimestamp = "2021-001T00:00:00.000"
  const planEndTimestamp = "2021-005T00:00:00.000"

  const planId = await createPlan(missionModelId, await generateUniquePlanName(ssoToken), planStartTimestamp, planEndTimestamp, ssoToken)
  await insertActivity(planId, getPostgresIntervalString(planStartTimestamp, "2021-002T00:00:00.000"),
      "HP3TemP",
      {},
      ssoToken)
  const goalId = await insertSchedulingGoal(missionModelId, "Schedule DSN contacts for initial setup", ssoToken)
  const planRevision = await getPlanRevision(planId, ssoToken)
  const specificationId = await insertSchedulingSpecification(planId, planRevision, planStartTimestamp, planEndTimestamp, {}, ssoToken)
  const affectedRows = await setSchedulingSpecificationGoals(specificationId, [goalId], ssoToken)
  assert(affectedRows === 1)
  await triggerSchedulingRun(specificationId, ssoToken)
}

async function login(username: string, password: string): Promise<string> {
  /**
   * Login to the Aerie Gateway and get an SSO Token
   */
  const response: Response = await fetch(LOGIN_URL, {
    method: 'post',
    body: JSON.stringify({username, password}),
    headers: {'Content-Type': 'application/json'}
  })
  if (!response.ok) {
    throw Error(response.statusText)
  }
  const json$: unknown = await response.json()
  if (typeof json$ !== "object" || json$ === null || !json$.hasOwnProperty("ssoToken") || (json$ as { ssoToken: string })["ssoToken"] === null) {
    throw new Error("Failed to login: " + JSON.stringify(json$))
  }

  return (json$ as { ssoToken: string })["ssoToken"]
}

async function uploadFile(filepath: string, filename: string, ssoToken: string): Promise<number> {
  const blob = fileFromSync(filepath, 'application/octet-stream')
  const formData = new FormData()
  formData.append('file', blob, filename)
  const response = await fetch(FILES_API_URL, {
    method: 'POST',
    body: formData,
    headers: {"x-auth-sso-token": ssoToken}
  })
  if (!response.ok) {
    throw Error(response.statusText)
  }
  const json = await response.json() as { id: number }
  return json["id"]
}

async function ensureMissionModelHasBeenUploaded(missionModelMetadata: { mission: string, name: string, version: string }, ssoToken: string, pathToJar: string) {
  let missionModelId
  const modelIds = await checkMissionModelExists(missionModelMetadata, ssoToken)
  if (modelIds.length > 0) {
    missionModelId = modelIds[0]
    console.log("Using existing mission model jar with id " + missionModelId)
  } else {
    console.log("Uploading mission model jar")
    const jarId = await uploadFile(pathToJar, "aerielander-0.10.0-SNAPSHOT.jar", ssoToken)
    const response = await query(
        `
          mutation CreateModel($model: mission_model_insert_input!) {
            createModel: insert_mission_model_one(object: $model) {
              id
            }
          }`,
        {
          "model": {
            ...missionModelMetadata,
            jar_id: jarId
          }
        },
        ssoToken) as { createModel: { id: number } }
    missionModelId = response["createModel"]["id"]
    console.log("Using newly uploaded mission model jar with id " + missionModelId)
  }
  return missionModelId
}

async function checkMissionModelExists({mission, name, version}: MissionModelMetadata, ssoToken: string) {
  const response = await query(`
    query MissionModelExists($mission: String!, $name: String!, $version: String!) {
      mission_model(where: {_and: {mission: {_eq: $mission}, name: {_eq: $name}, version: {_eq: $version}}}) {
        id
      }
    }
  `,
      {mission, name, version},
      ssoToken) as { mission_model: { id: number }[] }
  return response["mission_model"].map(x => x["id"])
}

async function query(queryDefinition: string, variables: object, ssoToken: string) {
  const response = await fetch(GRAPHQL_API_URL, {
    method: 'post',
    body: JSON.stringify({
      query: queryDefinition,
      variables
    }),
    headers: {
      "x-auth-sso-token": ssoToken
    }
  })
  if (!response.ok) {
    throw Error(response.statusText)
  }
  const json = await response.json() as { data: object }
  if (!json.hasOwnProperty("data")) {
    throw Error(JSON.stringify(json))
  }
  return json['data']
}

async function generateUniquePlanName(ssoToken: string) {
  const response = await query(`
  query GetPlans {
    plan {
      name
    }
  }
  `, {}, ssoToken) as { plan: { name: string }[] }
  const takenNames = response["plan"].map(x => x["name"])
  let i = 0
  let newPlanName
  while (true) {
    newPlanName = `my_plan_${i}`
    if (!takenNames.includes(newPlanName)) {
      break
    }
    i = i + 1
  }
  return newPlanName
}

async function createPlan(missionModelId: number, planName: string, startTimestamp: string, endTimestamp: string, ssoToken: string) {
  const response = await query(
      `
  mutation CreatePlan($plan: plan_insert_input!) {
    createPlan: insert_plan_one(object: $plan) {
      id
      revision
    }
  }
  `,
      {
        "plan": {
          "model_id": missionModelId,
          "name": planName,
          "start_time": startTimestamp,
          "duration": getPostgresIntervalString(startTimestamp, endTimestamp)
        },
      },
      ssoToken) as { createPlan: { id: number } }
  const planId = response["createPlan"]["id"]

  await query(
      `
  mutation CreateSimulation($simulation: simulation_insert_input!) {
    createSimulation: insert_simulation_one(object: $simulation) {
      id
    }
  }
  `,
      {
        "simulation": {
          "arguments": {},
          "plan_id": planId
        }
      },
      ssoToken
  )

  return planId
}

async function insertActivity(plan_id: number, start_offset: string, type: string, args: object, ssoToken: string) {
  const response = (await query(`
     mutation CreateActivity($activity: activity_insert_input!) {
      createActivity: insert_activity_one(object: $activity) {
        id
      }
    }
  `,
      {
        "activity": {
          "plan_id": plan_id,
          "start_offset": start_offset,
          "type": type,
          "arguments": args
        }
      },
      ssoToken
  )) as { createActivity: { id: number } }
  return response["createActivity"]["id"]
}

async function insertSchedulingGoal(model_id: number, definition: string, ssoToken: string) {
  //TODO iron out whether definition is a string or a jsonb
  const response = await query(
      `
mutation MakeSchedulingGoal($definition: jsonb, $model_id: Int) {
  insert_scheduling_goal_one(object: {definition: $definition, model_id: $model_id}) {
    id
  }
}
`,
      {
        "definition": definition,
        "model_id": model_id
      }, ssoToken) as { insert_scheduling_goal_one: { id: number } }
  return response["insert_scheduling_goal_one"]["id"]
}

async function getPlanRevision(planId: number, ssoToken: string) {
  const response = await query(
      `
query GetPlanRevision($plan_id: Int!) {
  plan_by_pk(id: $plan_id) {
    revision
  }
}
`,
      {
        "plan_id": planId
      }, ssoToken
  ) as { plan_by_pk: { revision: number } }
  return response["plan_by_pk"]["revision"]
}

async function insertSchedulingSpecification(planId: number, planRevision: number, horizonStart: string, horizonEnd: string, simulationArguments: object, ssoToken: string) {
  const response = await query(
      `
mutation MakeSchedulingSpec($plan_id: Int!, $plan_revision: Int!, $horizon_start: timestamptz, $horizon_end: timestamptz, $simulation_arguments: jsonb) {
  insert_scheduling_specification_one(object: {
    plan_id: $plan_id,
        plan_revision: $plan_revision,
        horizon_start: $horizon_start,
        horizon_end: $horizon_end,
        simulation_arguments: $simulation_arguments}
) {
    id
  }
}
`,
      {
        "plan_id": planId,
        "plan_revision": planRevision,
        "horizon_start": horizonStart,
        "horizon_end": horizonEnd,
        "simulation_arguments": simulationArguments
      }, ssoToken) as { insert_scheduling_specification_one: { id: number } }
  return response["insert_scheduling_specification_one"]["id"]
}

async function setSchedulingSpecificationGoals(specificationId: number, goalIds: number[], ssoToken: string) {
// scheduling spec is expected to be empty - priority starts at 0.
  const response = await query(
      `
mutation AddGoalsToSchedulingSpec($objects: [scheduling_specification_goals_insert_input!]!) {
  insert_scheduling_specification_goals(objects:$objects) {
    affected_rows
  }
}
`,
      {
        "objects": goalIds.map((goal_id, i) => ({
          "goal_id": goal_id,
          "specification_id": specificationId,
          "priority": i
        }))
      },
      ssoToken
  ) as { insert_scheduling_specification_goals: { affected_rows: number } }
  return response["insert_scheduling_specification_goals"]["affected_rows"]
}

async function triggerSchedulingRun(spec_id: number, ssoToken: string) {
  const response = await query(
      `
query TriggerSchedulingRun($spec_id:Int!) {
  schedule(specificationId:$spec_id){
    status
    reason
  }
}
`,
      {
        "spec_id": spec_id
      },
      ssoToken
  ) as { schedule: { status: string, reason: string } }["schedule"]
  if (response.hasOwnProperty("reason")) {
    throw Error(JSON.stringify(response))
  }
  console.log(response)
}

function getPostgresIntervalString(startTime: string, endTime: string): string {
  // Constructs a PostgresQL interval from two stringified datetimes
  const fmt = `yyyy-ooo'T'HH:mm:ss'.'SSS`
  const milliseconds: number = DateTime.fromFormat(endTime, fmt).diff(DateTime.fromFormat(startTime, fmt)).as('milliseconds')
  return `${Math.floor(milliseconds / 1000)} seconds ${milliseconds % 1000} milliseconds`
}

main()
