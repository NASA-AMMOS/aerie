import { gql, GraphQLClient } from 'graphql-request';
import { TimingTypes } from '../src/lib/codegen/CommandEDSLPreface';

let graphqlClient: GraphQLClient;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string);
});

describe('getEdslForSeqJson', () => {
    it('should return the seqjson for a given sequence edsl', async () => {

      const res = await graphqlClient.request<{
        getEdslForSeqJson: string
      }>(
        gql`
          query GetEdslForSeqJson($seqJson: SequenceSeqJsonInput!) {
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
            ]
          },
        },
      );

      expect(res.getEdslForSeqJson).toEqual(
`export default () =>
  Sequence.new({
    seqId: 'test_00001',
    metadata: {},
    commands: [
      C.BAKE_BREAD,
      A\`2020-060T03:45:19.000\`.PREHEAT_OVEN(100),
    ],
  });`
      );

    });
});

describe('getEdslForSeqJsonBulk', () => {
  it('should return the seqjson for a given sequence edsl', async () => {

    const res = await graphqlClient.request<{
      getEdslForSeqJsonBulk: string
    }>(
      gql`
        query GetEdslForSeqJsonBulk($seqJsons: [SequenceSeqJsonInput!]!) {
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
                args: [100],
                metadata: {},
              },
            ]
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
                args: [100],
                metadata: {},
              },
            ]
          },
        ],
      },
    );

    expect(res.getEdslForSeqJsonBulk).toEqual([
`export default () =>
  Sequence.new({
    seqId: 'test_00001',
    metadata: {},
    commands: [
      C.BAKE_BREAD,
      A\`2020-060T03:45:19.000\`.PREHEAT_OVEN(100),
    ],
  });`,
`export default () =>
  Sequence.new({
    seqId: 'test_00002',
    metadata: {},
    commands: [
      C.BAKE_BREAD,
      A\`2020-060T03:45:19.000\`.PREHEAT_OVEN(100),
    ],
  });`,
    ]);

  });
});
