import { GraphQLClient } from 'graphql-request';
import { insertCommandDictionary } from './testUtils/CommandDictionary.js';

let graphqlClient: GraphQLClient;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string, {
    headers: { 'x-hasura-admin-secret': process.env['HASURA_GRAPHQL_ADMIN_SECRET'] as string },
  });
});

describe('upload command dictionary', () => {
  it('should upload a command dictionary and all of the fields should be populated correctly', async () => {
    expect(await insertCommandDictionary(graphqlClient)).toBeDefined();
  }, 30000);
});
