import { GraphQLClient } from 'graphql-request';
import { insertExpansion, removeExpansion } from '../utils/Expansion';
import { expansionBatchLoader } from '../../src/lib/batchLoaders/expansionBatchLoader';

let graphqlClient: GraphQLClient;
let expansionId: number;

beforeAll(async () => {
  graphqlClient = new GraphQLClient(process.env.MERLIN_GRAPHQL_URL as string);
  expansionId = await insertExpansion(graphqlClient, 'PeelBanana', `export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
  return [
    PREHEAT_OVEN({temperature: 70}),
    PREPARE_LOAF(50, false),
    BAKE_BREAD,
  ];
}`);
});

afterAll(async () => {
  await removeExpansion(graphqlClient, expansionId);
});

it('should load expansion data', async () => {
  const expansions = await expansionBatchLoader({ graphqlClient })([{ expansionId }]);
  if (expansions[0] instanceof Error) {
    throw expansions[0];
  }
  expect(expansions[0].activityType).toBe('PeelBanana');
  expect(expansions[0].expansionLogic).toBe(`export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
  return [
    PREHEAT_OVEN({temperature: 70}),
    PREPARE_LOAF(50, false),
    BAKE_BREAD,
  ];
}`);
});
