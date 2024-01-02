import { sleep, check } from 'k6';
import { Trend } from 'k6/metrics';
import type {Options} from 'k6/options';
import { req } from './requests';
import gql from '../assets/gql';
import type {User} from "./types/auth";
import {CreatePlanInput} from "./types/plan";
import {ActivityInsertInput} from "./types/activity";
import {EffectiveArgsData, EffectiveArgumentItem} from "./types/effective_args";

// Besides the default metrics k6 records, we can define our own statistics and manually add data points to them
const effective_args_duration = new Trend('effective_arg_duration', true);

type VUSharedData = {
  user: User
  targeted_plan_id: number
  model_id: number
};

// The options object configures our overall load test
// https://k6.io/docs/using-k6/k6-options/reference
export const options: Options = {
  // The following scenarios are defined, which all run in parallel
  scenarios: {
    insert_plans: {
      // This defines the function that this scenario will run
      exec: 'insert_plans',

      // "VUs" (virtual users) are threads that run the function we referenced above in a loop, simulating real users.
      // Pre-allocating VUs simply saves on run-time resources
      preAllocatedVUs: 50,

      // When the executor is `ramping-arrival-rate`, the stages define the number of requests
      // per second we want to target. The k6 scheduler will then automatically scale the number of VUs
      // in order to hit that desired rate

      // https://k6.io/docs/using-k6/scenarios/executors/
      executor: 'ramping-arrival-rate',
      stages: [
        // Start 10 iterations per second for the first 30 seconds,
        { target: 10, duration: '30s' },
        // Then, linearly scale up to 50 iterations/second for a minute,
        { target: 50, duration: '1m' },
        // Then scale down again over 30 seconds.
        { target: 10, duration: '30s' },
      ],
    },

    insert_random_activities: {
      exec: 'insert_random_activities',
      executor: 'ramping-arrival-rate',
      // This scenario will only start after 5 seconds, allowing plans to populate first
      startTime: "5s",
      preAllocatedVUs: 50,
      stages: [
        { target: 10, duration: '30s' },
        { target: 50, duration: '1m' },
        { target: 10, duration: '30s' },
      ],
    },

    insert_targeted_activities: {
      exec: 'insert_targeted_activities',
      // Here we use a different executor, which keeps VUs constant, running as many iterations as
      // possible for a given duration
      executor: 'constant-vus',
      startTime: '5s',
      vus: 50,
      duration: '30s'
    },

    run_targeted_simulations: {
      exec: 'run_targeted_simulations',
      startTime: '1m',
      executor: 'constant-vus',
      vus: 2,
      duration: "30s"
    },

    run_random_simulations: {
      exec: 'run_random_simulations',
      startTime: '10s',
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 10,
      stages: [
        { target: 5, duration: '30s' },
        { target: 10, duration: '1m' },
        { target: 5, duration: '30s' },
      ],
    },

    get_effective_args: {
      exec: 'get_effective_args',
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 10,
      stages: [
        { target: 5, duration: '30s' },
        { target: 10, duration: '1m' },
        { target: 5, duration: '30s' },
      ],
    },
  },

  // If the statistics of the load test fall below the following threshold, it with exit with a nonzero exit code
  thresholds: {
    http_req_failed: ['rate<0.01'], // http errors should be less than 1%
    http_req_duration: ['p(50)<200', 'p(95)<1000'], // 50% below 200ms, 95% of requests should be below 1000ms
  },
};

const plan_start_timestamp = '2021-001T00:00:00.000';
const jar = open('./banananation.jar', 'b')

// `setup()` runs once per k6 instance, and sets up a shared mission model for all VUs
// VUSharedData is a custom type we return from the `setup()` function
// This data is then accessible as an argument for other load testing functions
export function setup(): VUSharedData {
  const username = "load-tester";
  const user = req.login(username);
  check(user, {
    'user has correct name': (user) => user.username === username
  });

  const modelId = req.uploadMissionModel(jar, user);
  sleep(2);

  const planInput: CreatePlanInput = {
    model_id: modelId,
    name: "load test targeted plan",
    start_time: plan_start_timestamp,
    duration: "1d"
  }
  const planId = req.createPlan(planInput, user);

  return {
    user,
    targeted_plan_id: planId,
    model_id: modelId,
  };
}

export function teardown(data: VUSharedData) {
  // 3. teardown code
  // Runs once after all tests are finished

  // Remove all plans using the uploaded model
  req.removePlansForModel(data.model_id, data.user);
  // Remove mission model
  req.removeModel(data.model_id, data.user);
}

export function insert_plans(data: VUSharedData) {
  const { user, model_id } = data;
  // generate new randomness per iteration
  const rd = Math.random() * 10000;

  const input: CreatePlanInput = {
    model_id: model_id,
    name: `test_plan_${rd}`,
    start_time: plan_start_timestamp,
    duration: "1d"
  };

  const plan_id = req.createPlan(input, user);
  check(plan_id, {
    "plan_id is a number": (plan_id) => Number.isInteger(plan_id)
  });
}

export function insert_random_activities(data: VUSharedData) {
  const { user, model_id } = data;
  const random_hour_offset = Math.round(Math.random() * 24);

  const random_id = req.get_random_plan_id(model_id, user);

  const input: ActivityInsertInput = {
    arguments: {
      biteSize: 1,
    },
    plan_id: random_id,
    type: 'BiteBanana',
    start_offset: random_hour_offset + 'h',
  };

  req.createActivityDirective(input, user);
}

// "Targeted" means insert lots of activities to plan_id 1
export function insert_targeted_activities(data: VUSharedData) {
  const { user, targeted_plan_id: planId } = data;
  const random_seconds_offset = Math.round(Math.random() * 24 * 60 * 60);

  const input: ActivityInsertInput = {
    arguments: {
      biteSize: 1,
    },
    plan_id: planId,
    type: 'BiteBanana',
    start_offset: random_seconds_offset + 's',
  };

  req.createActivityDirective(input, user);
}

// Targeted simulations only request simulations on plan_id 1, which is packed full of activities
export function run_targeted_simulations(data: VUSharedData) {
  const { user, targeted_plan_id: planId } = data;
  req.simulate(planId, user);
}

export function run_random_simulations(data: VUSharedData) {
  const { user, model_id } = data;
  const random_id = req.get_random_plan_id(model_id, user);
  req.simulate(random_id, user);
}

export function get_effective_args(data: VUSharedData) {
  const { user, model_id } = data;

  const input: EffectiveArgumentItem = {
    activityTypeName: "BiteBanana",
    activityArguments: {}
  };

  const resp = req.hasura(gql.GET_EFFECTIVE_ACTIVITY_ARGUMENTS_BULK, { modelId: model_id, activities: input }, user);

  // we are able to manually add data points to our custom metrics as follows
  effective_args_duration.add(resp.timings.duration);

  const effective_args_data = resp.json("data") as EffectiveArgsData;
  const effective_args = effective_args_data.getActivityEffectiveArgumentsBulk[0];

  check(effective_args, {
    "was successful": (effective_args) => effective_args.success
  });
}
