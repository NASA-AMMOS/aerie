import fs from 'fs';
import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';
import type { BatchLoader } from './index.js';
import { gql, GraphQLClient } from 'graphql-request';

export const commandDictionaryTypescriptBatchLoader: BatchLoader<
  { dictionaryId: number },
  string,
  { graphqlClient: GraphQLClient }
> = opts => async keys => {
  const { command_dictionary } = await opts.graphqlClient.request<{
    command_dictionary: {
      id: number;
      command_types_typescript_path: string;
    }[];
  }>(
    gql`
      query GetCommandDictionaries($dictionaryIds: [Int!]!) {
        command_dictionary(where: { id: { _in: $dictionaryIds } }) {
          id
          command_types_typescript_path
        }
      }
    `,
    {
      dictionaryIds: keys.map(key => key.dictionaryId),
    },
  );

  return Promise.all(
    keys.map(async ({ dictionaryId }) => {
      const dict = command_dictionary.find(({ id }) => id.toString() === dictionaryId.toString());
      if (dict === undefined) {
        return new ErrorWithStatusCode(`No dictionary with id: ${dictionaryId}`, 404);
      }
      return fs.promises.readFile(dict.command_types_typescript_path, 'utf8');
    }),
  );
};
