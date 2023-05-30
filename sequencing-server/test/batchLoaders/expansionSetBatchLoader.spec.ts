import type { GraphQLClient } from 'graphql-request';
import { expansionSetBatchLoader } from '../../src/lib/batchLoaders/expansionSetBatchLoader.js';
import { removeMissionModel, uploadMissionModel } from '../testUtils/MissionModel.js';
import { insertExpansion, insertExpansionSet, removeExpansion, removeExpansionSet } from '../testUtils/Expansion';
import { insertCommandDictionary, removeCommandDictionary } from '../testUtils/CommandDictionary';
import { getGraphQLClient } from '../testUtils/testUtils.js';

let graphqlClient: GraphQLClient;
let missionModelId: number;
let expansionId: number;
let expansionSetId: number;
let commandDictionaryId: number;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
  commandDictionaryId = (await insertCommandDictionary(graphqlClient)).id;
});

beforeAll(async () => {
  missionModelId = await uploadMissionModel(graphqlClient);
  expansionId = await insertExpansion(
    graphqlClient,
    'ParameterTest',
    `
  export default function ParameterTestExpansion() {
    return BAKE_BREAD;
  }
  `,
  );
  expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);
});

afterAll(async () => {
  await removeExpansionSet(graphqlClient, expansionSetId);
  await removeExpansion(graphqlClient, expansionId);
  await removeCommandDictionary(graphqlClient, commandDictionaryId);
  await removeMissionModel(graphqlClient, missionModelId);
  await removeMissionModel(graphqlClient, missionModelId);
});

// We expect this to fail right now because the expansion set batch loader needs to pull the dictionary commands from
// the filesystem, which isn't available locally, but in the docker container.
it.skip('[XFAIL] should load expansion set data', async () => {
  const expansionSets = await expansionSetBatchLoader({ graphqlClient })([{ expansionSetId }]);
  if (expansionSets[0] instanceof Error) {
    throw expansionSets[0];
  }
  expect(expansionSets[0]?.expansionRules[0]?.activityType).toBe('ParameterTest');
});
