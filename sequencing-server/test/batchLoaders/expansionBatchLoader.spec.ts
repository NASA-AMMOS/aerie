import type { GraphQLClient } from 'graphql-request';
import { insertExpansion, removeExpansion } from '../testUtils/Expansion';
import { expansionBatchLoader } from '../../src/lib/batchLoaders/expansionBatchLoader';
import { getGraphQLClient } from '../testUtils/testUtils';
import { insertCommandDictionary, removeCommandDictionary } from '../testUtils/CommandDictionary';
import { insertParcel, removeParcel } from '../testUtils/Parcel';

let graphqlClient: GraphQLClient;
let expansionId: number;
let commandDictionaryId: number;
let parcelId: number;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
  commandDictionaryId = (await insertCommandDictionary(graphqlClient)).id;
  parcelId = (await insertParcel(graphqlClient, commandDictionaryId, 'expansionBatchLoaderTestParcel')).parcelId;

  expansionId = await insertExpansion(
    graphqlClient,
    'PeelBanana',
    `export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
  return [
    PREHEAT_OVEN({temperature: 70}),
    PREPARE_LOAF(50, false),
    BAKE_BREAD,
  ];
}`,
    parcelId,
  );
});

afterAll(async () => {
  await removeExpansion(graphqlClient, expansionId);
  await removeParcel(graphqlClient, parcelId);
  await removeCommandDictionary(graphqlClient, commandDictionaryId);
});

it('should load expansion data', async () => {
  const expansions = await expansionBatchLoader({ graphqlClient })([{ expansionId }]);
  if (expansions[0] instanceof Error) {
    throw expansions[0];
  }
  expect(expansions[0]?.activityType).toBe('PeelBanana');
  expect(expansions[0]?.expansionLogic)
    .toBe(`export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
  return [
    PREHEAT_OVEN({temperature: 70}),
    PREPARE_LOAF(50, false),
    BAKE_BREAD,
  ];
}`);
});
