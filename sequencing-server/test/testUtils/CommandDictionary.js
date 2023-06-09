import fs from 'node:fs';
import { gql } from 'graphql-request';
import { randomUUID } from 'node:crypto';
export const commandDictionaryString = fs.readFileSync(new URL('../../cdict/command_banananation.xml', import.meta.url).pathname, 'utf-8');
export async function insertCommandDictionary(graphqlClient) {
    const res = await graphqlClient.request(gql `
      mutation PutCommandDictionary($dictionary: String!) {
        uploadDictionary(dictionary: $dictionary) {
          id
          command_types_typescript_path
          mission
          version
          parsed_json
        }
      }
    `, {
        // Generate a UUID for the command dictionary name and version to avoid conflicts when testing.
        dictionary: commandDictionaryString.replace(/(Banana Nation|1.0.0.0)/g, randomUUID()),
    });
    return res.uploadDictionary;
}
export async function removeCommandDictionary(graphqlClient, commandDictionaryId) {
    return graphqlClient.request(gql `
      mutation DeleteCommandDictionary($commandDictionaryId: Int!) {
        delete_command_dictionary_by_pk(id: $commandDictionaryId) {
          id
        }
      }
    `, {
        commandDictionaryId,
    });
}
//# sourceMappingURL=CommandDictionary.js.map