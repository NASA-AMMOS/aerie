import { gql, GraphQLClient } from 'graphql-request';
import { randomUUID } from 'crypto';

export async function createPlan(graphqlClient: GraphQLClient, missionModelId: number): Promise<number> {
  /*
   * Create a plan
   */
  const res = await graphqlClient.request(gql`
    mutation InsertTestPlan($plan: plan_insert_input! = {}) {
      insert_plan_one(object: $plan) {
        id
      }
    }
  `, {
    plan: {
      name: 'banana republic' + randomUUID(),
      start_time: '2020-001T00:00:00',
      duration: '86400 seconds 0 milliseconds',
      model_id: missionModelId,
    }
  });
  return res.insert_plan_one.id;
}

export async function removePlan(graphqlClient: GraphQLClient, planId: number): Promise<void> {
  /*
   * Remove a plan
   */

  await graphqlClient.request(gql`
    mutation DeleteTestPlan($planId: Int!) {
      delete_plan_by_pk(id: $planId) {
        id
      }
    }
  `, {
    planId,
  });
}
