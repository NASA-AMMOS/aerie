import { sleep } from "k6";
import { req } from './requests';
import type { User } from "./types/auth";
import {CreatePlanInput} from "./types/plan";
import {ActivityInsertInput} from "./types/activity";

export const options = {
  // A number specifying the number of VUs to run concurrently.
  vus: 50,
  // A string specifying the total duration of the test run.
  duration: '10s',

  // If the statistics of the load test fall below the following threshold, it with exit with a nonzero exit code
  thresholds: {
    http_req_failed: ['rate<0.01'], // http errors should be less than 1%
    http_req_duration: ['p(50)<300', 'p(95)<1000'], // 50% below 300ms, 95% of requests should be below 1000ms
  },
};

type VUSharedData = {
  user: User,
  planId: number,
  modelId: number
}

// 'open' is only permitted in the 'init' stage in k6 (the scope that sets up globals)
// http requests are NOT permitted in the 'init' stage in k6
// therefore, we must grab the jars during the init stage and upload them during the setup stage
const jar = open('./banananation.jar', 'b')

export function setup() : VUSharedData {
  // 1. setup code
  // Runs once
  // Login
  const user = req.login('lockup_tester');

  // Upload a model
  const modelId = req.uploadMissionModel(jar, user);

  // Upload initial plan
  const planInput: CreatePlanInput = {
    model_id: modelId,
    name: "lockup test plan",
    start_time: "2024-01-01T00:00:00+00:00",
    duration: "400:00:00"
  };
  const planId = req.createPlan(planInput, user);

  // Upload one activity per hour
  for (let hour = 0; hour < 400; hour++) {
    const activityInput: ActivityInsertInput = {
      plan_id: planId,
      type: "BiteBanana",
      arguments: {},
      start_offset: hour+":00:00"
    }
    req.createActivityDirective(activityInput, user);
  }

  return { user: user, planId: planId, modelId: modelId };
}

export default function (data: VUSharedData) {
  // 2. VU code
  // Runs once per iteration
  req.duplicatePlan(data.planId, "duplicate_loadtest_branch_" +Math.trunc(Math.random() * 10000000).toString(), data.user);
  sleep(1);
}


export function teardown(data: VUSharedData) {
  // 3. teardown code
  // Runs once

  // Remove parent plan
  req.removePlan(data.planId, data.user);
  // Remove all plans using the uploaded model
  req.removePlansForModel(data.modelId, data.user);
  // Remove mission model
  req.removeModel(data.modelId, data.user);
}

