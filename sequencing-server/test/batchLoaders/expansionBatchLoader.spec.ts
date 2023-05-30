import type { GraphQLClient } from 'graphql-request';
import { insertExpansion, removeExpansion } from '../testUtils/Expansion';
import { expansionBatchLoader } from '../../src/lib/batchLoaders/expansionBatchLoader';
import { getGraphQLClient } from '../testUtils/testUtils';

let graphqlClient: GraphQLClient;
let expansionId: number;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
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
  );
});

afterAll(async () => {
  await removeExpansion(graphqlClient, expansionId);
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
