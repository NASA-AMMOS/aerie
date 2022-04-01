import { GraphQLClient } from 'graphql-request';
import { expansionSetBatchLoader } from '../../src/lib/batchLoaders/expansionSetBatchLoader.js';
import { removeMissionModel, uploadMissionModel } from '../utils/MissionModel.js';
import { insertExpansion, insertExpansionSet, removeExpansion, removeExpansionSet } from '../utils/Expansion';
import { insertCommandDictionary, removeCommandDictionary } from '../utils/CommandDictionary';

let graphqlClient: GraphQLClient;
let missionModelId: number;
let expansionId: number;
let expansionSetId: number;
let commandDictionaryId: number;

beforeAll(async () => {
  graphqlClient = new GraphQLClient(process.env.MERLIN_GRAPHQL_URL as string);
  missionModelId = await uploadMissionModel(graphqlClient);
  commandDictionaryId = await insertCommandDictionary(graphqlClient);
  expansionId = await insertExpansion(graphqlClient);
  expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);
});

// afterAll(async () => {
//   await removeExpansionSet(graphqlClient, expansionSetId);
//   await removeExpansion(graphqlClient, expansionId);
//   await removeCommandDictionary(graphqlClient, commandDictionaryId);
//   await removeMissionModel(graphqlClient, missionModelId);
//   await removeMissionModel(graphqlClient, missionModelId);
// });

it('should load expansion set data', async () => {
  const expansionSets = await expansionSetBatchLoader({graphqlClient})([
    { expansionSetId },
  ]);
  if (expansionSets[0] instanceof Error) {
    throw expansionSets[0];
  }
  expect(expansionSets[0].expansionRules[0].activityType).toBe('ParameterTest');
});
