import fs from 'node:fs';
import { gql, GraphQLClient } from 'graphql-request';

const commandDictionaryString = fs.readFileSync(
    new URL('../inputs/command-dictionary.xml', import.meta.url).pathname,
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
      dictionary: commandDictionaryString,
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
