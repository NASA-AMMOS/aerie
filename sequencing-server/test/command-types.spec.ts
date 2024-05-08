import { gql, GraphQLClient } from 'graphql-request';
import { Status } from '../src/common.js';
import { insertCommandDictionary, removeCommandDictionary } from './testUtils/CommandDictionary.js';
import { getGraphQLClient } from './testUtils/testUtils.js';

let graphqlClient: GraphQLClient;
let commandDictionaryId: number;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
  commandDictionaryId = (await insertCommandDictionary(graphqlClient)).id;
});

afterAll(async () => {
  removeCommandDictionary(graphqlClient, commandDictionaryId);
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
  expect(getCommandTypeScript.typescriptFiles.length).toEqual(4);
  expect(getCommandTypeScript.typescriptFiles).toEqual([
    {
      content: expect.any(String),
      filePath: 'command-types.ts',
    },
    {
      content: expect.any(String),
      filePath: 'TemporalPolyfillTypes.ts',
    },
    {
      content: expect.any(String),
      filePath: 'ChannelTypes.ts',
    },
    {
      content: expect.any(String),
      filePath: 'ParameterTypes.ts',
    },
  ]);
  expect(getCommandTypeScript.typescriptFiles[0]!.content).toMatchSnapshot();
  expect(getCommandTypeScript.reason).toBe(null);
}, 10000);
