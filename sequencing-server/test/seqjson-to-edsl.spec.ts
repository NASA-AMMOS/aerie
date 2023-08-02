import { gql, GraphQLClient } from 'graphql-request';
import { TimingTypes } from '../src/lib/codegen/CommandEDSLPreface';
import { getGraphQLClient } from './testUtils/testUtils';

let graphqlClient: GraphQLClient;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
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
    steps: ({ locals, parameters }) => ([
      C.BAKE_BREAD,
      A\`2020-060T03:45:19.000\`.PREHEAT_OVEN(100),
    ]),
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
          id: 'all_possible_fields',
          metadata: {
            onboard_path: '/eng',
            onboard_name: 'test.mod',
            lgo: { boolean: false },
            other_arbitrary_metadata: 'test_metadata',
          },
          locals: [
            {
              name: 'local_float_1',
              type: 'FLOAT',
            },
            {
              name: 'local_int_2',
              type: 'INT',
            },
            {
              name: 'local_string_3',
              type: 'STRING',
            },
            {
              name: 'local_uint_4',
              type: 'UINT',
            },
          ],
          parameters: [
            {
              name: 'param_float_1',
              type: 'FLOAT',
            },
            {
              name: 'param_int_2',
              type: 'INT',
            },
            {
              name: 'param_string_3',
              type: 'STRING',
            },
            {
              name: 'param_uint_4',
              type: 'UINT',
            },
          ],
          steps: [
            {
              args: [
                { type: 'number', value: 30 },
                { type: 'number', value: 4.3 },
                { type: 'boolean', value: true },
                { type: 'string', value: 'test_string' },
                {
                  type: 'repeat',
                  value: [
                    [
                      { type: 'number', value: 10 },
                      { type: 'string', value: 'another_test' },
                      { type: 'boolean', value: false },
                    ],
                    [
                      { type: 'number', value: 5 },
                      { type: 'string', value: 'repeat_test' },
                      { type: 'boolean', value: true },
                    ],
                  ],
                },
              ],
              description: 'Epoch-relative activate step for test.mod into engine 2 with all possible fields.',
              engine: 2,
              epoch: 'TEST_EPOCH',
              models: [
                {
                  offset: '00:00:00.000',
                  variable: 'model_var_float',
                  value: '1.234',
                },
                {
                  offset: '00:00:00.001',
                  variable: 'model_var_int',
                  value: '-1234',
                },
                {
                  offset: '01:02:03.000',
                  variable: 'model_var_string',
                  value: 'Model test string',
                },
                {
                  offset: '10:00:00.000',
                  variable: 'model_var_uint',
                  value: '1234',
                },
              ],
              sequence: 'd:/eng/test.mod',
              time: { tag: '00:00:01.000', type: 'EPOCH_RELATIVE' },
              type: 'activate',
            },
            {
              args: [
                { type: 'number', value: 30 },
                { type: 'number', value: 4.3 },
                { type: 'boolean', value: true },
                { type: 'string', value: 'test_string' },
                {
                  type: 'repeat',
                  value: [
                    [
                      { type: 'number', value: 10 },
                      { type: 'string', value: 'another_test' },
                    ],
                  ],
                },
              ],
              description: 'Absolute-timed standard command step with all possible fields.',
              models: [
                {
                  offset: '00:00:00.000',
                  variable: 'model_var_float',
                  value: '1.234',
                },
                {
                  offset: '00:00:00.001',
                  variable: 'model_var_int',
                  value: '-1234',
                },
                {
                  offset: '01:02:03.000',
                  variable: 'model_var_string',
                  value: 'Model test string',
                },
                {
                  offset: '10:00:00.000',
                  variable: 'model_var_uint',
                  value: '1234',
                },
              ],
              stem: 'FAKE_COMMAND1',
              time: { tag: '2020-173T20:00:00.000', type: 'ABSOLUTE' },
              type: 'command',
              metadata: {},
            },
            {
              args: [
                { type: 'string', value: 'SEQSTR' },
                { type: 'string', value: '2019-365T00:00:00' },
                { type: 'string', value: '2020-025T00:00:00' },
                { type: 'string', value: 'BOTH' },
                { type: 'string', value: '' },
                { type: 'string', value: '' },
                { type: 'string', value: 'real_time_cmds' },
              ],
              description: 'Ground activity step with required fields.',
              models: [
                {
                  offset: '00:00:00.000',
                  variable: 'model_var_float',
                  value: '1.234',
                },
                {
                  offset: '00:00:00.001',
                  variable: 'model_var_int',
                  value: '-1234',
                },
                {
                  offset: '01:02:03.000',
                  variable: 'model_var_string',
                  value: 'Model test string',
                },
                {
                  offset: '10:00:00.000',
                  variable: 'model_var_uint',
                  value: '1234',
                },
              ],
              name: 'SEQTRAN_directive',
              time: { tag: '00:00:01.000', type: 'COMMAND_RELATIVE' },
              type: 'ground_block',
              metadata: { stringfield: 'stringval' },
            },
            {
              args: [
                {
                  type: 'string',
                  value: '/domops/data/nsyt/189/seq/satf_sct/nsy.orf.f2_seq_eng_nom_htr_off_mod.r1.satf',
                },
                { type: 'string', value: 'd:/tmp/eng_nom_htr_off.mod' },
              ],
              description: 'Ground event step with all possible fields.',
              models: [
                {
                  offset: '00:00:00.000',
                  variable: 'model_var_float',
                  value: '1.234',
                },
                {
                  offset: '00:00:00.001',
                  variable: 'model_var_int',
                  value: '-1234',
                },
                {
                  offset: '01:02:03.000',
                  variable: 'model_var_string',
                  value: 'Model test string',
                },
                {
                  offset: '10:00:00.000',
                  variable: 'model_var_uint',
                  value: '1234',
                },
              ],
              name: 'UPLINK_SEQUENCE_FILE',
              time: { type: 'COMMAND_COMPLETE' },
              type: 'ground_event',
              metadata: { listfield: ['1', 2] },
            },
            {
              args: [
                { type: 'symbol', value: 'Local_Var_A' },
                { type: 'symbol', value: 'Global_Var_B' },
              ],
              description: 'Epoch-relative activate step for test.mod into engine 2 with all possible fields.',
              engine: 2,
              epoch: 'TEST_EPOCH',
              models: [
                {
                  offset: '00:00:00.000',
                  variable: 'model_var_float',
                  value: '1.234',
                },
                {
                  offset: '00:00:00.001',
                  variable: 'model_var_int',
                  value: '-1234',
                },
                {
                  offset: '01:02:03.000',
                  variable: 'model_var_string',
                  value: 'Model test string',
                },
                {
                  offset: '10:00:00.000',
                  variable: 'model_var_uint',
                  value: '1234',
                },
              ],
              sequence: 'd:/eng/test.mod',
              time: { tag: '00:00:01.000', type: 'EPOCH_RELATIVE' },
              type: 'load',
            },
          ],
          requests: [
            {
              description: 'Absolute-timed request object with all possible fields',
              name: 'test_request1',
              steps: [
                {
                  stem: 'FAKE_COMMAND1',
                  time: { tag: '00:00:01.000', type: 'COMMAND_RELATIVE' },
                  type: 'command',
                  args: [],
                },
              ],
              time: { tag: '2020-173T20:00:00.000', type: 'ABSOLUTE' },
              type: 'request',
            },
            {
              description: 'Ground-epoch timed request object with all possible fields',
              ground_epoch: { delta: '+00:30:00', name: 'test_ground_epoch' },
              name: 'test_request1',
              steps: [
                {
                  stem: 'FAKE_COMMAND1',
                  time: { tag: '00:00:01.000', type: 'COMMAND_RELATIVE' },
                  type: 'command',
                  args: [],
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
    seqId: 'all_possible_fields',
    metadata: {
      lgo:{
        boolean: false,
      },
      onboard_name: 'test.mod',
      onboard_path: '/eng',
      other_arbitrary_metadata: 'test_metadata',
    },
    locals: [
      FLOAT('local_float_1'),
      INT('local_int_2'),
      STRING('local_string_3'),
      UINT('local_uint_4')
    ],
    parameters: [
      FLOAT('param_float_1'),
      INT('param_int_2'),
      STRING('param_string_3'),
      UINT('param_uint_4')
    ],
    steps: ({ locals, parameters }) => ([
      E\`00:00:01.000\`.ACTIVATE('d:/eng/test.mod')
        .ARGUMENTS(30,4.3,true,'test_string',[[10,'another_test',false],[5,'repeat_test',true]])
        .DESCRIPTION('Epoch-relative activate step for test.mod into engine 2 with all possible fields.')
        .ENGINE(2)
        .EPOCH('TEST_EPOCH'),
      A\`2020-173T20:00:00.000\`.FAKE_COMMAND1(30,4.3,true,'test_string',[[10,'another_test']])
        .DESCRIPTION('Absolute-timed standard command step with all possible fields.')
        .MODELS([
          {
            offset: '00:00:00.000',
            value: '1.234',
            variable: 'model_var_float',
          },
          {
            offset: '00:00:00.001',
            value: '-1234',
            variable: 'model_var_int',
          },
          {
            offset: '01:02:03.000',
            value: 'Model test string',
            variable: 'model_var_string',
          },
          {
            offset: '10:00:00.000',
            value: '1234',
            variable: 'model_var_uint',
          }
        ]),
      R\`00:00:01.000\`.GROUND_BLOCK('SEQTRAN_directive')
        .ARGUMENTS('SEQSTR','2019-365T00:00:00','2020-025T00:00:00','BOTH','','','real_time_cmds')
        .DESCRIPTION('Ground activity step with required fields.')
        .METADATA({
          stringfield: 'stringval',
        })
        .MODELS([
          {
            offset: '00:00:00.000',
            value: '1.234',
            variable: 'model_var_float',
          },
          {
            offset: '00:00:00.001',
            value: '-1234',
            variable: 'model_var_int',
          },
          {
            offset: '01:02:03.000',
            value: 'Model test string',
            variable: 'model_var_string',
          },
          {
            offset: '10:00:00.000',
            value: '1234',
            variable: 'model_var_uint',
          }
        ]),
      C.GROUND_EVENT('UPLINK_SEQUENCE_FILE')
        .ARGUMENTS('/domops/data/nsyt/189/seq/satf_sct/nsy.orf.f2_seq_eng_nom_htr_off_mod.r1.satf','d:/tmp/eng_nom_htr_off.mod')
        .DESCRIPTION('Ground event step with all possible fields.')
        .METADATA({
          listfield: [
              '1',
              2,
          ],
        })
        .MODELS([
          {
            offset: '00:00:00.000',
            value: '1.234',
            variable: 'model_var_float',
          },
          {
            offset: '00:00:00.001',
            value: '-1234',
            variable: 'model_var_int',
          },
          {
            offset: '01:02:03.000',
            value: 'Model test string',
            variable: 'model_var_string',
          },
          {
            offset: '10:00:00.000',
            value: '1234',
            variable: 'model_var_uint',
          }
        ]),
      E\`00:00:01.000\`.LOAD('d:/eng/test.mod')
        .ARGUMENTS(unknown.Local_Var_A //ERROR: Variable 'Local_Var_A' is not defined as a local or parameter
        ,unknown.Global_Var_B //ERROR: Variable 'Global_Var_B' is not defined as a local or parameter
        )
        .DESCRIPTION('Epoch-relative activate step for test.mod into engine 2 with all possible fields.')
        .ENGINE(2)
        .EPOCH('TEST_EPOCH'),
    ]),
    requests: [
      A\`2020-173T20:00:00.000\`.REQUEST(
        'test_request1',
        R\`00:00:01.000\`.FAKE_COMMAND1
      )
        .DESCRIPTION('Absolute-timed request object with all possible fields'),
      REQUEST(
        'test_request1',
        {
          delta: '+00:30:00',
          name: 'test_ground_epoch',
        },
        R\`00:00:01.000\`.FAKE_COMMAND1
      )
        .DESCRIPTION('Ground-epoch timed request object with all possible fields')
    ],
  });`);
  });

  it('should return errors in edsl', async () => {
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
          id: '',
          locals: [
            {
              name: 'temp',
              type: 'FLOAT',
            },
          ],
          metadata: {},
          parameters: [
            {
              name: 'sugar',
              type: 'INT',
            },
          ],
          steps: [
            {
              args: [
                {
                  name: 'temperature',
                  type: 'symbol',
                  value: 'temp',
                },
              ],
              stem: 'PREHEAT_OVEN',
              time: {
                type: 'COMMAND_COMPLETE',
              },
              type: 'command',
            },
            {
              args: [
                {
                  name: 'tb_sugar',
                  type: 'symbol',
                  value: 'sugarrrrr',
                },
                {
                  name: 'gluten_free',
                  type: 'string',
                  value: 'FALSE',
                },
              ],
              stem: 'PREPARE_LOAF',
              time: {
                type: 'COMMAND_COMPLETE',
              },
              type: 'command',
            },
          ],
        },
      },
    );

    expect(res.getEdslForSeqJson).toEqual(`export default () =>
  Sequence.new({
    seqId: '',
    metadata: {},
    locals: [
      FLOAT('temp')
    ],
    parameters: [
      INT('sugar')
    ],
    steps: ({ locals, parameters }) => ([
      C.PREHEAT_OVEN(locals.temp),
      C.PREPARE_LOAF(unknown.sugarrrrr //ERROR: Variable 'sugarrrrr' is not defined as a local or parameter
      ,'FALSE'),
    ]),
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
      "export default () =>\n  Sequence.new({\n    seqId: 'test_00001',\n    metadata: {},\n    steps: ({ locals, parameters }) => ([\n      C.BAKE_BREAD,\n      A`2020-060T03:45:19.000`.PREHEAT_OVEN(100),\n    ]),\n  });",
      "export default () =>\n  Sequence.new({\n    seqId: 'test_00002',\n    metadata: {},\n    steps: ({ locals, parameters }) => ([\n      C.BAKE_BREAD,\n      A`2020-060T03:45:19.000`.PREHEAT_OVEN(100),\n    ]),\n  });"
    ]);
  });
});
