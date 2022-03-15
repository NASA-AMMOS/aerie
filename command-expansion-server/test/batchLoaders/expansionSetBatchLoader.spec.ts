import { GraphQLClient } from 'graphql-request';
import { expansionSetBatchLoader } from '../../src/lib/batchLoaders/expansionSetBatchLoader.js';
import { removeMissionModel, uploadMissionModel } from '../utils/MissionModel.js';

let graphqlClient: GraphQLClient;
let missionModelId: number;

beforeAll(async () => {
  graphqlClient = new GraphQLClient(process.env.MERLIN_GRAPHQL_URL as string);
  missionModelId = await uploadMissionModel(graphqlClient);
});

afterAll(async () => {
  await removeMissionModel(graphqlClient, missionModelId);
});

it('should load expansion set data', async () => {
  const expansionSets = await expansionSetBatchLoader({graphqlClient})([
    { simulationDatasetId },
  ]);
  if (expansionSets[0] instanceof Error) {
    throw expansionSets[0];
  }
  expect(expansionSets[0][0].type).toBe('ParameterTest');
});
