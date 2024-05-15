import fs from 'node:fs';
import { gql, GraphQLClient } from 'graphql-request';
import { randomUUID } from 'node:crypto';
import type { CommandDictionary } from '@nasa-jpl/aerie-ampcs';

export const commandDictionaryString = fs.readFileSync(
  new URL('../../cdict/command_banananation.xml', import.meta.url).pathname,
  'utf-8',
);

export async function insertCommandDictionary(graphqlClient: GraphQLClient): Promise<{
  id: number;
  dictionary_path: string;
  mission: string;
  version: string;
  parsed_json: CommandDictionary;
}> {
  const res = await graphqlClient.request<{
    uploadDictionary: {
      id: number;
      dictionary_path: string;
      mission: string;
      version: string;
      parsed_json: CommandDictionary;
    };
  }>(
    gql`
      mutation PutCommandDictionary($dictionary: String!, $type: String!) {
        uploadDictionary(dictionary: $dictionary, type: $type) {
          id
          dictionary_path
          mission
          version
          parsed_json
        }
      }
    `,
    {
      // Generate a UUID for the command dictionary name and version to avoid conflicts when testing.
      dictionary: commandDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, randomUUID()),
      type: 'COMMAND',
    },
  );

  return res.uploadDictionary;
}

export async function removeCommandDictionary(
  graphqlClient: GraphQLClient,
  commandDictionaryId: number,
): Promise<void> {
  return graphqlClient.request(
    gql`
      mutation DeleteCommandDictionary($commandDictionaryId: Int!) {
        delete_command_dictionary_by_pk(id: $commandDictionaryId) {
          id
        }
      }
    `,
    {
      commandDictionaryId,
    },
  );
}
