import DataLoader from 'dataloader';
import { gql, GraphQLClient } from 'graphql-request';
import { activitySchemaBatchLoader } from '../../src/lib/batchLoaders/activitySchemaBatchLoader';
import { simulatedActivitiesBatchLoader } from '../../src/lib/batchLoaders/simulatedActivityBatchLoader';
import { assertDefined } from '../../src/utils/assertions';

export async function insertActivity(
  graphqlClient: GraphQLClient,
  planId: number,
  activityType: string,
  startOffset: string = '30 seconds 0 milliseconds',
): Promise<number> {
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
      activityType,
      startOffset: startOffset,
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

export async function convertActivityIdToSimulatedActivityId(
  graphqlClient: GraphQLClient,
  simulationDatasetId: number,
  activityId: number,
): Promise<number> {
  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }));

  const simulatedActivities = (
    await simulatedActivitiesBatchLoader({ graphqlClient, activitySchemaDataLoader })([{ simulationDatasetId }])
  )[0];

  if (simulatedActivities instanceof Error) {
    throw simulatedActivities;
  }

  return assertDefined(
    simulatedActivities.find(simulatedActivity => simulatedActivity.attributes.directiveId === activityId),
  ).id;
}
