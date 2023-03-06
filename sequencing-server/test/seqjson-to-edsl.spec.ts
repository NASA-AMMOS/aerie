import { gql, GraphQLClient } from 'graphql-request';
import { TimingTypes } from '../src/lib/codegen/CommandEDSLPreface';

let graphqlClient: GraphQLClient;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string);
});

describe('getEdslForSeqJson', () => {
  it('should return the seqjson for a given sequence edsl', async () => {
    const res = await graphqlClient.request<{
      getEdslForSeqJson: string;
    }>(
      gql`
        query GetEdslForSeqJson($seqJson: SequenceSeqJson!) {
          getEdslForSeqJson(seqJson: $seqJson)
        }
      `,
      {
        seqJson: {
          id: 'test_00001',
          metadata: {},
          steps: [
            {
              // expansion 1
              type: 'command',
              stem: 'BAKE_BREAD',
              time: { type: TimingTypes.COMMAND_COMPLETE },
              args: [],
              metadata: {},
            },
            {
              type: 'command',
              stem: 'PREHEAT_OVEN',
              time: { type: TimingTypes.ABSOLUTE, tag: '2020-060T03:45:19.000Z' },
              args: [{ value: 100, name: 'temperature', type: 'number' }],
              metadata: {},
            },
          ],
        },
      },
    );

    expect(res.getEdslForSeqJson).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test_00001',
    metadata: {},
    steps: [
      C.BAKE_BREAD,
      A\`2020-060T03:45:19.000\`.PREHEAT_OVEN({
        temperature: 100,
      }),
    ],
  });`);
  });

  it('should throw an error if the user uploads an invalid seqjson', async () => {
    try {
      expect(
        await graphqlClient.request<{
          getEdslForSeqJson: string;
        }>(
          gql`
            query GetEdslForSeqJson($seqJson: SequenceSeqJson!) {
              getEdslForSeqJson(seqJson: $seqJson)
            }
          `,
          {
            seqJson: {
              id: 'test_00001',
              metadata: {},
              steps: [
                {
                  // expansion 1
                  type: 'command',
                  stem: 'BAKE_BREAD',
                  time: { type: TimingTypes.COMMAND_COMPLETE },
                  args: [],
                  metadata: {},
                },
                {
                  type: 'command',
                  stem: 'PREHEAT_OVEN',
                  time: { type: TimingTypes.ABSOLUTE, tag: '2020-060T03:45:19.000Z' },
                  args: [100],
                  metadata: {},
                },
              ],
            },
          },
        ),
      ).toThrow();
    } catch (e) {}
  });
});

describe('getEdslForSeqJsonBulk', () => {
  it('should return the seqjson for a given sequence edsl', async () => {
    const res = await graphqlClient.request<{
      getEdslForSeqJsonBulk: string;
    }>(
      gql`
        query GetEdslForSeqJsonBulk($seqJsons: [SequenceSeqJson!]!) {
          getEdslForSeqJsonBulk(seqJsons: $seqJsons)
        }
      `,
      {
        seqJsons: [
          {
            id: 'test_00001',
            metadata: {},
            steps: [
              {
                // expansion 1
                type: 'command',
                stem: 'BAKE_BREAD',
                time: { type: TimingTypes.COMMAND_COMPLETE },
                args: [],
                metadata: {},
              },
              {
                type: 'command',
                stem: 'PREHEAT_OVEN',
                time: { type: TimingTypes.ABSOLUTE, tag: '2020-060T03:45:19.000Z' },
                args: [{ value: 100, name: 'temperature', type: 'number' }],
                metadata: {},
              },
            ],
          },
          {
            id: 'test_00002',
            metadata: {},
            steps: [
              {
                // expansion 1
                type: 'command',
                stem: 'BAKE_BREAD',
                time: { type: TimingTypes.COMMAND_COMPLETE },
                args: [],
                metadata: {},
              },
              {
                type: 'command',
                stem: 'PREHEAT_OVEN',
                time: { type: TimingTypes.ABSOLUTE, tag: '2020-060T03:45:19.000Z' },
                args: [{ value: 100, name: 'temperature', type: 'number' }],
                metadata: {},
              },
            ],
          },
        ],
      },
    );

    expect(res.getEdslForSeqJsonBulk).toEqual([
      "export default () =>\n  Sequence.new({\n    seqId: 'test_00001',\n    metadata: {},\n    steps: [\n      C.BAKE_BREAD,\n      A`2020-060T03:45:19.000`.PREHEAT_OVEN({\n        temperature: 100,\n      }),\n    ],\n  });",
      "export default () =>\n  Sequence.new({\n    seqId: 'test_00002',\n    metadata: {},\n    steps: [\n      C.BAKE_BREAD,\n      A`2020-060T03:45:19.000`.PREHEAT_OVEN({\n        temperature: 100,\n      }),\n    ],\n  });",
    ]);
  });
});
