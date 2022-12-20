import DataLoader from 'dataloader';
import { gql, GraphQLClient } from 'graphql-request';
import { activitySchemaBatchLoader } from '../../src/lib/batchLoaders/activitySchemaBatchLoader';
import { simulatedActivitiesBatchLoader } from '../../src/lib/batchLoaders/simulatedActivityBatchLoader';
import { assertDefined } from '../../src/utils/assertions';

export async function insertActivityDirective(
  graphqlClient: GraphQLClient,
  planId: number,
  activityType: string,
  startOffset: string = '30 seconds 0 milliseconds',
  args: any = {},
): Promise<number> {
  const res = await graphqlClient.request<{
    insert_activity_directive_one: { id: number };
  }>(
    gql`
      mutation InsertTestActivityDirective(
        $activityType: String!
        $planId: Int!
        $startOffset: interval!
        $arguments: jsonb
      ) {
        insert_activity_directive_one(
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
      arguments: args,
    },
  );
  return res.insert_activity_directive_one.id;
}

export async function removeActivityDirective(
  graphqlClient: GraphQLClient,
  activityId: number,
  planId: number,
): Promise<void> {
  return graphqlClient.request(
    gql`
      mutation DeleteActivityDirective($activityId: Int!, $planId: Int!) {
        delete_activity_directive_by_pk(id: $activityId, plan_id: $planId) {
          id
          plan_id
        }
      }
    `,
    {
      activityId,
      planId,
    },
  );
}

export async function convertActivityDirectiveIdToSimulatedActivityId(
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
    simulatedActivities?.find(simulatedActivity => simulatedActivity.attributes.directiveId === activityId),
  ).id;
}
