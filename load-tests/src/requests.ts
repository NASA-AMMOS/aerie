import http, { RefinedResponse, ResponseType } from "k6/http";
import * as urls from "../../e2e-tests/src/utilities/urls";
import { JSONObject, check } from "k6";

export const req = {
  hasura(
    query: string,
    variables: JSONObject,
    token: string,
    username: string
  ): RefinedResponse<ResponseType | undefined> {

    const headers = {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'x-hasura-role': 'aerie_admin',
      'x-hasura-user-id': `${username}`
    };

    const body =
      JSON.stringify({
        query,
        variables
    });

    const response = http.post(`${urls.HASURA_URL}/v1/graphql`, body, { headers });

    if (response.error) {
      console.error(response.error);
    }

    return response;
  },

  get_random_plan_id(token: string, username: string) {
    const query = `#graphql
      query {
        plan {
          id
        }
      }
    `;

    const resp = req.hasura(query, {}, token, username);
    const data = resp.json("data");

    const plan_ids = data.plan;
    check(plan_ids, {
      "plans exists": (plan_ids) => plan_ids.length > 0
    });

    const random_plan = plan_ids[Math.floor(Math.random() * plan_ids.length)];
    check(random_plan, {
      "random plan exists": (random_plan) => random_plan !== undefined
    });

    return random_plan.id;
  },

  get_random_mission_model_id(token: string, username: string) {
    const query = `#graphql
      query {
        mission_model {
          id
        }
      }
    `;

    const resp = req.hasura(query, {}, token, username);
    const data = resp.json("data");

    const mission_models: = data.mission_model;
    check(mission_models, {
      "mission model exists": (mission_models) => mission_models.length > 0
    });

    const random_mission = mission_models[Math.floor(Math.random() * mission_models.length)];
    check(random_mission, {
      "random mission exists": (random_mission) => random_mission !== undefined
    });

    return random_mission.id;
  },

  login(username: string) {
    const response = http.post(`${urls.UI_URL}/auth/login`,
    `{
      "username": "${username}",
      "password": "${username}"
    }`,
    {
      headers: {
        'content-type': 'application/json',
      },
    });

    const login_resp = response.json();
    return login_resp.user;
  }
};
