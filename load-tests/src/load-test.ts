import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend } from 'k6/metrics';
import { Options } from 'k6/options';
import { req } from './requests';
import gql from '../../e2e-tests/src/utilities/gql';
import * as urls from '../../e2e-tests/src/utilities/urls';

// Besides the default metrics k6 records, we can define our own statistics and manually add data points to them
const effective_args_duration = new Trend('effective_arg_duration', true);

type VUSharedData = {
  token: string
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
const jar = open('./banananation.jar', 'b');
const username = "load-tester";
// this randomness is run once per VU
const rd = Math.random() * 10000;

// `setup()` runs once per k6 instance, and sets up a shared mission model for all VUs
// VUSharedData is a custom type we return from the `setup()` function
// This data is then accessible as an argument for other load testing functions
export function setup(): VUSharedData {

  const user = req.login(username);
  const token = user.token;

  check(user, {
    'user has correct name': (user) => user.id === username
  });

  // upload banananation jar
  const file = {
    type: "application/java-archive",
    file: http.file(jar)
  };

  const res = http.post(`${urls.GATEWAY_URL}/file`, file);

  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  const jar_data = res.json();
  const jar_id  = jar_data.id;

  check(jar_id, {
    "valid id": (id) => Number.isInteger(id)
  });

  // create mission model
  const modelInput: MissionModelInsertInput = {
    jar_id,
    mission: "aerie-load-test" + rd,
    name: "Banananation (load-test)"+rd,
    version: "0.0.0"+ rd,
  };

  const model_resp = req.hasura(gql.CREATE_MISSION_MODEL, {model: modelInput}, token, username);
  const model_data = model_resp.json("data");
  check(model_data, {
    "model id is valid": (data) => Number.isInteger(data.insert_mission_model_one.id)
  });

  sleep(2);

  return {
    token
  };
}

export function insert_plans(data: VUSharedData) {
  const { token } = data;
  // generate new randomness per iteration
  const rd = Math.random() * 10000;

  const random_mission_id = req.get_random_mission_model_id(token, username);

  const input: CreatePlanInput = {
    model_id: random_mission_id,
    name: `test_plan_${rd}_${random_mission_id}`,
    start_time: plan_start_timestamp,
    duration: "1d"
  };

  const resp = req.hasura(gql.CREATE_PLAN, {plan: input}, token, username);
  const plan_data = resp.json("data");
  const { insert_plan_one } = plan_data;
  const plan_id = insert_plan_one.id;

  check(plan_id, {
    "plan_id is a number": (plan_id) => Number.isInteger(plan_id)
  });
}

export function insert_random_activities(data: VUSharedData) {
  const { token } = data;
  const random_hour_offset = Math.round(Math.random() * 24);

  const random_id = req.get_random_plan_id(token, username);

  const input: ActivityInsertInput = {
    arguments: {
      biteSize: 1,
    },
    plan_id: random_id,
    type: 'BiteBanana',
    start_offset: random_hour_offset + 'h',
  };

  const resp = req.hasura(gql.CREATE_ACTIVITY_DIRECTIVE, {activityDirectiveInsertInput: input}, token, username);
  const act_data = resp.json("data");
  const { createActivityDirective } = act_data;
  const act_id = createActivityDirective.id;

  check(act_id, {
    "act dir id is a number": (act_id) => Number.isInteger(act_id)
  });
}

// "Targeted" means insert lots of activities to plan_id 1
export function insert_targeted_activities(data: VUSharedData) {
  const { token } = data;
  const random_seconds_offset = Math.round(Math.random() * 24 * 60 * 60);

  const input: ActivityInsertInput = {
    arguments: {
      biteSize: 1,
    },
    plan_id: 1,
    type: 'BiteBanana',
    start_offset: random_seconds_offset + 's',
  };

  const resp = req.hasura(gql.CREATE_ACTIVITY_DIRECTIVE, {activityDirectiveInsertInput: input}, token, username);
  const act_data = resp.json("data");
  const { createActivityDirective } = act_data;
  const act_id = createActivityDirective.id;

  check(act_id, {
    "act dir id is a number": (act_id) => Number.isInteger(act_id)
  });
}

// Targeted simulations only request simulations on plan_id 1, which is packed full of activities
export function run_targeted_simulations(data: VUSharedData) {
  const { token } = data;

  const resp = req.hasura(gql.SIMULATE, {plan_id: 1}, token, username);

  const sim_data = resp.json("data");

  check(sim_data, {
    "got sim dataset id": (sim_data) => Number.isInteger(sim_data.simulate.simulationDatasetId)
  });
}

export function run_random_simulations(data: VUSharedData) {
  const { token } = data;
  const random_id = req.get_random_plan_id(token, username);

  const resp = req.hasura(gql.SIMULATE, {plan_id: random_id}, token, username);

  const sim_data = resp.json("data");

  check(sim_data, {
    "got sim dataset id": (sim_data) => Number.isInteger(sim_data.simulate.simulationDatasetId)
  });
}

export function get_effective_args(data: VUSharedData) {
  const { token } = data;

  const input: EffectiveArgumentItem = {
    activityTypeName: "BiteBanana",
    activityArguments: {}
  };

  const random_id = req.get_random_mission_model_id(token, username);

  const resp = req.hasura(gql.GET_EFFECTIVE_ACTIVITY_ARGUMENTS_BULK, { modelId: random_id, activities: input }, token, username);

  // we are able to manually add data points to our custom metrics as follows
  effective_args_duration.add(resp.timings.duration);

  const effective_args_data = resp.json("data");
  const effective_args = effective_args_data.getActivityEffectiveArgumentsBulk[0];

  check(effective_args, {
    "was successful": (effective_args) => effective_args.success
  });
}
