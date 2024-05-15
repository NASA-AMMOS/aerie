import * as ampcs from '@nasa-jpl/aerie-ampcs';
import type { GraphQLClient } from 'graphql-request';
import {
  commandDictionaryString,
  insertCommandDictionary,
  removeCommandDictionary,
} from './testUtils/CommandDictionary.js';
import { getGraphQLClient } from './testUtils/testUtils.js';

let graphqlClient: GraphQLClient;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
});

describe('upload command dictionary', () => {
  it('should upload a command dictionary and all of the fields should be populated correctly', async () => {
    // During the test we use a uuid for the mission so there's no conflicting command dictionaries.
    const { id, dictionary_path, mission, parsed_json } = await insertCommandDictionary(graphqlClient);

    expect(dictionary_path).toBe(`/usr/src/app/sequencing_file_store/${mission}/command_lib.${mission}.ts`);

    expect(parsed_json).toStrictEqual(
      ampcs.parse(commandDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, mission)),
    );

    await removeCommandDictionary(graphqlClient, id);
  }, 30000);
});
