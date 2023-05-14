import fs from 'node:fs';
import { gql, GraphQLClient } from 'graphql-request';
import { randomUUID } from 'node:crypto';

const commandDictionaryString = fs.readFileSync(
  new URL('../../cdict/command_banananation.xml', import.meta.url).pathname,
  'utf-8',
);

export async function insertCommandDictionary(graphqlClient: GraphQLClient): Promise<number> {
  const res = await graphqlClient.request<{
    uploadDictionary: { id: number };
  }>(
    gql`
      mutation PutCommandDictionary($dictionary: String!) {
        uploadDictionary(dictionary: $dictionary) {
          id
        }
      }
    `,
    {
      // Generate a UUID for the command dictionary name and version to avoid conflicts when testing.
      dictionary: commandDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, randomUUID()),
    },
  );

  return res.uploadDictionary.id;
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
