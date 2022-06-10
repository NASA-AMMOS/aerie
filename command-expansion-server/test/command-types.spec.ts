import { gql, GraphQLClient } from 'graphql-request';
import { insertCommandDictionary, removeCommandDictionary } from './utils/CommandDictionary.js';
import { Status } from '../src/common.js';

let graphqlClient: GraphQLClient;
let commandDictionaryId: number;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env.MERLIN_GRAPHQL_URL as string);
  commandDictionaryId = await insertCommandDictionary(graphqlClient);
});

afterEach(async () => {
  await removeCommandDictionary(graphqlClient, commandDictionaryId);
});

it('should return command types', async () => {

  const { getCommandTypeScript } = await graphqlClient.request<{
    getCommandTypeScript: {
      status: Status;
      typescriptFiles: {
        content: string;
        filePath: string;
      }[];
      reason: string;
    };
  }>(
    gql`
      query GetCommandTypes($commandDictionaryId: Int!) {
        getCommandTypeScript(commandDictionaryId: $commandDictionaryId) {
          status
          typescriptFiles {
            content
            filePath
          }
          reason
        }
      }
    `,
    {
      commandDictionaryId,
    },
  );

  expect(getCommandTypeScript.status).toBe(Status.SUCCESS);
  expect(getCommandTypeScript.typescriptFiles.length).toEqual(1);
  expect(getCommandTypeScript.typescriptFiles).toEqual([
    {
      content: expect.any(String),
      filePath: 'command-types.ts',
    }
  ]);
  expect(getCommandTypeScript.reason).toBe(null);

}, 10000);
