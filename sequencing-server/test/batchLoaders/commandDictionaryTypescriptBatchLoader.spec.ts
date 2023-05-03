import { GraphQLClient } from 'graphql-request';
import { activitySchemaBatchLoader } from '../../src/lib/batchLoaders/activitySchemaBatchLoader.js';
import { removeMissionModel, uploadMissionModel } from '../testUtils/MissionModel.js';

let graphqlClient: GraphQLClient;
let missionModelId: number;

beforeAll(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string, {
    headers: { 'x-hasura-admin-secret': process.env['HASURA_GRAPHQL_ADMIN_SECRET'] as string },
  });
  missionModelId = await uploadMissionModel(graphqlClient);
}, 10000);
afterAll(async () => {
  await removeMissionModel(graphqlClient, missionModelId);
});

it('should load command typescript', async () => {
  const activitySchemas = await activitySchemaBatchLoader({ graphqlClient })([
    { activityTypeName: 'ParameterTest', missionModelId },
  ]);

  const firstSchema = activitySchemas[0];
  if (firstSchema instanceof Error) {
    throw firstSchema;
  }
  expect(firstSchema?.name).toBe('ParameterTest');
});
