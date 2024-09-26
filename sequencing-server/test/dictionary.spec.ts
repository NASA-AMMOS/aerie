import * as ampcs from '@nasa-jpl/aerie-ampcs';
import type { GraphQLClient } from 'graphql-request';
import {
  channelDictionaryString,
  commandDictionaryString,
  insertDictionary,
  parameterDictionaryString,
  removeDictionary,
} from './testUtils/Dictionary';
import { getGraphQLClient } from './testUtils/testUtils.js';
import { DictionaryType } from '../src/types/types';

let graphqlClient: GraphQLClient;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
});

describe('upload dictionaries', () => {
  it('should upload a command dictionary and all of the fields should be populated correctly', async () => {
    // During the test we use a uuid for the mission so there's no conflicting command dictionaries.
    const { id, dictionary_path, mission, parsed_json } = await insertDictionary(graphqlClient, DictionaryType.COMMAND);

    expect(dictionary_path).toBe(`/usr/src/app/sequencing_file_store/${mission}/command_lib.${mission}.ts`);

    expect(parsed_json).toStrictEqual(
      ampcs.parse(commandDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, mission)),
    );

    await removeDictionary(graphqlClient, id, DictionaryType.COMMAND);
  }, 30000);

  it('should upload a channel dictionary and all of the fields should be populated correctly', async () => {
    // During the test we use a uuid for the mission so there's no conflicting command dictionaries.
    const { id, dictionary_path, mission, parsed_json } = await insertDictionary(graphqlClient, DictionaryType.CHANNEL);

    expect(dictionary_path).toBe(`/usr/src/app/sequencing_file_store/${mission}/channel_lib.${mission}.ts`);

    expect(parsed_json).toEqual(
      ampcs.parseChannelDictionary(channelDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, mission)),
    );

    await removeDictionary(graphqlClient, id, DictionaryType.CHANNEL);
  }, 30000);

  it('should upload a parameter dictionary and all of the fields should be populated correctly', async () => {
    // During the test we use a uuid for the mission so there's no conflicting command dictionaries.
    const { id, dictionary_path, mission, parsed_json } = await insertDictionary(
      graphqlClient,
      DictionaryType.PARAMETER,
    );

    expect(dictionary_path).toBe(`/usr/src/app/sequencing_file_store/${mission}/parameter_lib.${mission}.ts`);

    expect(parsed_json).toEqual(
      ampcs.parseParameterDictionary(parameterDictionaryString.replace(/(Banana Nation|1.0.0.1)/g, mission)),
    );

    await removeDictionary(graphqlClient, id, DictionaryType.PARAMETER);
  }, 30000);
});
