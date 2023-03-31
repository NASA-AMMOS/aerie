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

  it('should return the full edsl', async () => {
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
          hardware_commands: [
            {
              description: 'FIRE THE PYROS',
              metadata: {
                author: 'rrgoetz',
              },
              stem: 'HDW_PYRO_ENGINE',
            },
          ],
          id: 'banana1001.0000a',
          immediate_commands: [
            {
              args: [
                {
                  name: 'direction',
                  type: 'string',
                  value: 'FromStem',
                },
              ],
              stem: 'PEEL_BANANA',
            },
          ],
          locals: [
            {
              allowable_ranges: [
                {
                  max: 3600,
                  min: 1,
                },
              ],
              name: 'duration',
              type: 'UINT',
            },
          ],
          metadata: {
            author: 'rrgoetz',
          },
          parameters: [
            {
              allowable_ranges: [
                {
                  max: 3600,
                  min: 1,
                },
              ],
              name: 'duration',
              type: 'UINT',
            },
          ],
          requests: [
            {
              description: ' Activate the oven',
              ground_epoch: {
                delta: 'now',
                name: 'activate',
              },
              metadata: {
                author: 'rrgoetz',
              },
              name: 'power',
              steps: [
                {
                  args: [
                    {
                      name: 'temperature',
                      type: 'number',
                      value: 360,
                    },
                  ],
                  stem: 'PREHEAT_OVEN',
                  time: {
                    tag: '04:39:22.000',
                    type: 'COMMAND_RELATIVE',
                  },
                  type: 'command',
                },
                {
                  args: [],
                  stem: 'ADD_WATER',
                  time: {
                    type: 'COMMAND_COMPLETE',
                  },
                  type: 'command',
                },
              ],
              type: 'request',
            },
            {
              description: ' Activate the water',
              ground_epoch: {
                delta: 'now',
                name: 'activate',
              },
              metadata: {
                author: 'rrgoetz',
              },
              name: 'water',
              steps: [
                {
                  args: [],
                  stem: 'ADD_WATER',
                  time: {
                    type: 'COMMAND_COMPLETE',
                  },
                  type: 'command',
                },
              ],
              type: 'request',
            },
          ],
        },
      },
    );

    expect(res.getEdslForSeqJson).toEqual(`export default () =>
  Sequence.new({
    seqId: 'banana1001.0000a',
    metadata: {
      author: 'rrgoetz',
    },
    locals: [
      {
        allowable_ranges: [
          {
            max: 3600,
            min: 1,
          },
        ],
        name: 'duration',
        type: 'UINT',
      }
    ],
    parameters: [
      {
        allowable_ranges: [
          {
            max: 3600,
            min: 1,
          },
        ],
        name: 'duration',
        type: 'UINT',
      }
    ],
    hardware_commands: [
      HDW_PYRO_ENGINE
      .DESCRIPTION('FIRE THE PYROS')
      .METADATA({
        author: 'rrgoetz',
      })
    ],
    immediate_commands: [
      PEEL_BANANA({
        direction: 'FromStem',
      }),
    ],
    requests: [
      {
        name: 'power',
        steps: [
          R\`04:39:22.000\`.PREHEAT_OVEN({
            temperature: 360,
          }),
          C.ADD_WATER,
        ],
        type: 'request',
        description: ' Activate the oven',
        ground_epoch: {
          delta: 'now',
          name: 'activate',
        },
        metadata: {
          author: 'rrgoetz',
        },
      },
      {
        name: 'water',
        steps: [
          C.ADD_WATER,
        ],
        type: 'request',
        description: ' Activate the water',
        ground_epoch: {
          delta: 'now',
          name: 'activate',
        },
        metadata: {
          author: 'rrgoetz',
        },
      }
    ],
  });`);
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
