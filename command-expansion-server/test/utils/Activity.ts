import { gql, GraphQLClient } from 'graphql-request';

export async function insertActivity(graphqlClient: GraphQLClient, planId: number): Promise<number> {
  const res = await graphqlClient.request<{
    insert_activity_one: { id: number };
  }>(
    gql`
      mutation InsertTestActivity($activityType: String!, $planId: Int!, $startOffset: interval!, $arguments: jsonb) {
        insert_activity_one(
          object: { type: $activityType, start_offset: $startOffset, plan_id: $planId, arguments: $arguments }
        ) {
          id
        }
      }
    `,
    {
      planId: planId,
      activityType: 'ParameterTest',
      startOffset: '0 seconds 0 milliseconds',
      arguments: {},
    },
  );
  return res.insert_activity_one.id;
}

export async function removeActivity(graphqlClient: GraphQLClient, activityId: number): Promise<void> {
  return graphqlClient.request(
    gql`
      mutation DeleteActivity($activityId: Int!) {
        delete_activity_by_pk(id: $activityId) {
          id
        }
      }
    `,
    {
      activityId,
    },
  );
}
