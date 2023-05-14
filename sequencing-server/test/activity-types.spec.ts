import { gql, GraphQLClient } from 'graphql-request';
import { Status } from '../src/common.js';
import { removeMissionModel, uploadMissionModel } from './testUtils/MissionModel';

let graphqlClient: GraphQLClient;
let missionModelId: number;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string, {
    headers: { 'x-hasura-admin-secret': process.env['HASURA_GRAPHQL_ADMIN_SECRET'] as string },
  });
  missionModelId = await uploadMissionModel(graphqlClient);
}, 10000);

afterEach(async () => {
  await removeMissionModel(graphqlClient, missionModelId);
});

it('should return activity types', async () => {
  const { getActivityTypeScript } = await graphqlClient.request<{
    getActivityTypeScript: {
      status: Status;
      typescriptFiles: {
        content: string;
        filePath: string;
      }[];
      reason: string;
    };
  }>(
    gql`
      query GetActivityTypes($missionModelId: Int!, $activityTypeName: String!) {
        getActivityTypeScript(missionModelId: $missionModelId, activityTypeName: $activityTypeName) {
          status
          typescriptFiles {
            content
            filePath
          }
          reason
        }
      }
    `,
    {
      missionModelId,
      activityTypeName: 'PeelBanana',
    },
  );

  expect(getActivityTypeScript.status).toBe(Status.SUCCESS);
  expect(getActivityTypeScript.typescriptFiles.length).toEqual(2);
  expect(getActivityTypeScript.typescriptFiles).toEqual([
    {
      content: expect.any(String),
      filePath: 'activity-types.ts',
    },
    {
      content: expect.any(String),
      filePath: 'TemporalPolyfillTypes.ts',
    },
  ]);
  expect(getActivityTypeScript.reason).toBe(null);
}, 10000);
