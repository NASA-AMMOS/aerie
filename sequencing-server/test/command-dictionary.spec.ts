import * as ampcs from '@nasa-jpl/aerie-ampcs';
import { GraphQLClient } from 'graphql-request';
import { commandDictionaryString, insertCommandDictionary } from './testUtils/CommandDictionary.js';

let graphqlClient: GraphQLClient;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string, {
    headers: { 'x-hasura-admin-secret': process.env['HASURA_GRAPHQL_ADMIN_SECRET'] as string },
  });
});

describe('upload command dictionary', () => {
  it('should upload a command dictionary and all of the fields should be populated correctly', async () => {
    // During the test we use a uuid for the mission so there's no conflicting command dictionaries.
    const { command_types_typescript_path, mission, parsed_json } = await insertCommandDictionary(graphqlClient);

    expect(command_types_typescript_path).toBe(
      `/usr/src/app/sequencing_file_store/${mission}/command_lib.${mission}.ts`,
    );

    expect(parsed_json).toStrictEqual(
      ampcs.parse(commandDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, mission)),
    );
  }, 30000);
});
