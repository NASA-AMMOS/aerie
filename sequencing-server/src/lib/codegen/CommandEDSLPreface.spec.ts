import {
  CommandStem,
  HardwareStem,
  doyToInstant,
  DOY_STRING,
  hmsToDuration,
  HMS_STRING,
  Sequence,
  TimingTypes,
  Ground_Event,
  Ground_Block,
  ImmediateStem,
    FLOAT,
    INT,
    UINT,
    STRING,
    ENUM,
    Variable
} from './CommandEDSLPreface';

describe('Command', () => {
  describe('fromSeqJson', () => {
    it('should parse a command seqjson', () => {
      const command = CommandStem.fromSeqJson({
        type: 'command',
        stem: 'test',
        metadata: {},
        args: [],
        time: { type: TimingTypes.COMMAND_COMPLETE },
      });

      expect(command).toBeInstanceOf(CommandStem);
      expect(command.stem).toBe('test');
      expect(command.GET_METADATA()).toEqual({});
      expect(command.arguments).toEqual([]);
    });
  });

  describe('toEDSLString()', () => {
    it('should handle absolute time tagged commands', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: ['string', 0, true],
        absoluteTime: doyToInstant('2020-001T00:00:00.000' as DOY_STRING),
      });

      expect(command.toEDSLString()).toEqual("A`2020-001T00:00:00.000`.TEST('string', 0, true)");
    });

    it('should handle relative time tagged commands', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: ['string', 0, true],
        relativeTime: hmsToDuration('00:00:00.000' as HMS_STRING),
      });

      expect(command.toEDSLString()).toEqual("R`00:00:00.000`.TEST('string', 0, true)");
    });

    it('should handle epoch relative time tagged commands', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: ['string', 0, true],
        epochTime: hmsToDuration('00:00:00.000' as HMS_STRING),
      });

      expect(command.toEDSLString()).toEqual("E`00:00:00.000`.TEST('string', 0, true)");
    });

    it('should handle command complete commands', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: ['string', 0, true],
      });

      expect(command.toEDSLString()).toEqual("C.TEST('string', 0, true)");
    });

    it('should handle commands without arguments', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: [],
      });

      expect(command.toEDSLString()).toEqual('C.TEST');

      const command2 = CommandStem.new({
        stem: 'TEST',
        arguments: {},
      });

      expect(command2.toEDSLString()).toEqual('C.TEST');
    });

    it('should convert to EDSL string with array arguments', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: ['string', 0, true],
        absoluteTime: doyToInstant('2020-001T00:00:00.000' as DOY_STRING),
      });

      expect(command.toEDSLString()).toEqual("A`2020-001T00:00:00.000`.TEST('string', 0, true)");
    });

    it('should convert to EDSL string with named arguments', () => {
      const command = CommandStem.new({
        stem: 'TEST',
        arguments: {
          string: 'string',
          number: 0,
          boolean: true,
        },
        absoluteTime: doyToInstant('2020-001T00:00:00.000' as DOY_STRING),
      });

      expect(command.toEDSLString()).toEqual(
        "A`2020-001T00:00:00.000`.TEST({\n  string: 'string',\n  number: 0,\n  boolean: true,\n})",
      );
    });

    it('should convert to EDSL string from ground event', () => {
      const groundEvent = Ground_Event.new({
        name: 'Ground Event Name',
        args: [{ name: 'name', type: 'string', value: 'hello' }],
        absoluteTime: doyToInstant('2020-001T00:00:00.000' as DOY_STRING),
        description: 'ground event description',
        metadata: { author: 'Emery' },
      });

      expect(groundEvent.toEDSLString()).toEqual(
        "A`2020-001T00:00:00.000`.GROUND_EVENT('Ground Event Name')\n" +
          '.ARGUMENTS([\n' +
          '  {\n' +
          "    name: 'name',\n" +
          "    type: 'string',\n" +
          "    value: 'hello',\n" +
          '  }\n' +
          '])\n' +
          ".DESCRIPTION('ground event description')\n" +
          '.METADATA({\n' +
          "  author: 'Emery',\n" +
          '})',
      );
    });

    it('should convert to EDSL string from ground block', () => {
      const groundBlock = Ground_Block.new({
        name: 'Ground Block Name',
        args: [{ name: 'turnOff', type: 'boolean', value: false }],
        description: 'ground block description',
        metadata: { author: 'Jasmine' },
      });

      expect(groundBlock.toEDSLString()).toEqual(
        "C.GROUND_BLOCK('Ground Block Name')\n" +
          '.ARGUMENTS([\n' +
          '  {\n' +
          "    name: 'turnOff',\n" +
          "    type: 'boolean',\n" +
          '    value: false,\n' +
          '  }\n' +
          '])\n' +
          ".DESCRIPTION('ground block description')\n" +
          '.METADATA({\n' +
          "  author: 'Jasmine',\n" +
          '})',
      );
    });

    it('should convert to EDSL string from Variable', () => {
      const float = Variable.new(FLOAT('float_name', {allowable_ranges : [{min: 0, max : 1}], allowable_values : [1,3], sc_name : 'Mission Control'}));
      expect(float.toSeqJson()).toEqual({
        allowable_ranges: [
          {
            "max": 1,
            "min": 0
          }
        ],
        allowable_values: [
          1,
          3
        ],
        sc_name : 'Mission Control',
        name: "float_name",
        type: "FLOAT"
      });

      const int = Variable.new(INT('int_name', {allowable_ranges : [{min: 2, max : 10}], allowable_values : [1,3], sc_name : 'Mission Control 2'}));
      expect(int.toSeqJson()).toEqual({
        allowable_ranges: [
          {
            "max": 10,
            "min": 2
          }
        ],
        allowable_values: [
          1,
          3
        ],
        sc_name : 'Mission Control 2',
        name: "int_name",
        type: "INT"
      });

      const uint = Variable.new(UINT('uint_name'));
      expect(uint.toSeqJson()).toEqual({ name: 'uint_name', type: 'UINT' });

      const string = Variable.new(STRING('string_name', {allowable_values : [5,12,43], sc_name : 'Mission Control 3'}));
      expect(string.toSeqJson()).toEqual({ name: 'string_name', type: 'STRING' , allowable_values: [
          5,
          12,
          43
        ],sc_name : 'Mission Control 3'});

      const enumm = Variable.new(ENUM('enum_name','ENUM_NAME', {allowable_ranges : [{min: 20, max : 100}], allowable_values : [1,30], sc_name : 'Mission Control 20'}));
      expect(enumm.toSeqJson()).toEqual({ name: 'enum_name', enum_name : 'ENUM_NAME', type: 'ENUM', allowable_ranges: [
          {
            "max": 100,
            "min": 20
          }
        ],
        allowable_values: [
          1,
          30
        ], sc_name : 'Mission Control 20' });
    });

    it('should convert to EDSL string from Command with Local/Parameter arguments', () => {
      const local = Variable.new(FLOAT('temp'));
      const command = CommandStem.new({
        arguments: [{ temperature: local }],
        stem: 'PREHEAT_OVEN',
      });
      expect(command.toSeqJson()).toEqual({
        args: [{ name: 'temperature', type: 'symbol', value: 'temp' }],
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        type: 'command',
      });
    });
  });
});

describe('Sequence', () => {
  describe('fromSeqJson', () => {
    it('should parse a sequence seqjson', () => {
      const sequence = Sequence.fromSeqJson({
        id: 'test00000',
        metadata: {},
        steps: [
          {
            type: 'command',
            stem: 'test',
            metadata: {},
            args: [],
            time: { type: TimingTypes.COMMAND_COMPLETE },
          },
          {
            type: 'command',
            stem: 'test2',
            metadata: {
              author: 'Mission Operation Engineer',
            },
            args: [
              { value: 'test_string', type: 'string', name: 'parameter1' },
              { value: 0, type: 'number', name: 'parameter2' },
              { value: true, type: 'boolean', name: 'parameter3' },
            ],
            time: { type: TimingTypes.ABSOLUTE, tag: '2020-001T00:00:00.000' as DOY_STRING },
          },
        ],
      });

      expect(sequence).toBeInstanceOf(Sequence);
      expect(sequence.id).toBe('test00000');
      expect(sequence.metadata).toEqual({});

      expect(sequence.steps?.length).toEqual(2);

      if (sequence.steps) {
        expect(sequence.steps[0]! as CommandStem).toBeInstanceOf(CommandStem);
        expect(sequence.steps[0]!.stem).toBe('test');
        expect(sequence.steps[0]!.GET_METADATA()).toEqual({});
        expect(sequence.steps[0]!.arguments).toEqual([]);

        expect(sequence.steps[1]!).toBeInstanceOf(CommandStem);
        expect(sequence.steps[1]!.stem).toBe('test2');
        expect(sequence.steps[1]!.GET_METADATA()).toEqual({
          author: 'Mission Operation Engineer',
        });
        expect(sequence.steps[1]!.arguments).toEqual({ parameter1: 'test_string', parameter2: 0, parameter3: true });
      }
    });
  });

  describe('toEDSLString()', () => {
    it('should convert with no commands', () => {
      const sequence = Sequence.new({
        seqId: 'test',
        metadata: {},
        steps: [],
      });

      expect(sequence.toEDSLString()).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test',
    metadata: {},
  });`);
    });

    it('should convert with commands', () => {
      const local = Variable.new(FLOAT('temp'));
      local.setKind('locals')
      const parameter = Variable.new(ENUM('duration', 'POSSIBLE_DURATION'));
      parameter.setKind('parameters')
      const sequence = Sequence.new({
        seqId: 'test',
        metadata: {},
        locals: [local],
        parameters: [parameter],
        steps: [
          CommandStem.new({
            stem: 'TEST',
            arguments: ['string', 0, true],
            absoluteTime: doyToInstant('2020-001T00:00:00.000' as DOY_STRING),
          }),
          CommandStem.new({
            stem: 'TEST',
            arguments: {
              string: 'string',
              number: 0,
              boolean: true,
            },
            absoluteTime: doyToInstant('2020-001T00:00:00.000' as DOY_STRING),
          }).METADATA({
            author: 'XXXXXXXXXXXXXXXXXXXXXXXXXX',
          }),
          CommandStem.new({
            stem: 'TEST',
            arguments: {
              temperature: local,
              duration: parameter,
            },

            absoluteTime: doyToInstant('2021-001T00:00:00.000' as DOY_STRING),
          }).METADATA({
            author: 'ZZZZ',
          }),
        ],
      });

      expect(sequence.toEDSLString()).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test',
    metadata: {},
    locals: [
      FLOAT('temp')
    ],
    parameters: [
      ENUM('duration', 'POSSIBLE_DURATION')
    ],
    steps: ({ locals, parameters }) => ([
      A\`2020-001T00:00:00.000\`.TEST('string', 0, true),
      A\`2020-001T00:00:00.000\`.TEST({
        string: 'string',
        number: 0,
        boolean: true,
      })
      .METADATA({
        author: 'XXXXXXXXXXXXXXXXXXXXXXXXXX',
      }),
      A\`2021-001T00:00:00.000\`.TEST({
        temperature: locals.temp,
        duration: parameters.duration,
      })
      .METADATA({
        author: 'ZZZZ',
      }),
    ]),
  });`);
    });

    it('should convert with Hardware,', () => {
      const sequence = Sequence.new({
        seqId: 'HW',
        metadata: {},
        hardware_commands: [
          HardwareStem.new({ stem: 'HW_PYRO_DUMP' }).DESCRIPTION('Fire the pyros').METADATA({ author: 'Emery' }),
        ],
      });

      expect(sequence.toEDSLString()).toEqual(
        'export default () =>\n' +
          '  Sequence.new({\n' +
          "    seqId: 'HW',\n" +
          '    metadata: {},\n' +
          '    hardware_commands: [\n' +
          '      HW_PYRO_DUMP\n' +
          "      .DESCRIPTION('Fire the pyros')\n" +
          '      .METADATA({\n' +
          "        author: 'Emery',\n" +
          '      })\n' +
          '    ],\n' +
          '  });',
      );
    });

    it('should convert with immediate commands,', () => {
      const sequence = Sequence.new({
        seqId: 'Immediate',
        metadata: {},
        immediate_commands: [
          ImmediateStem.new({ stem: 'SMASH_BANANA', arguments: ['AGRESSIVE'] })
            .DESCRIPTION('Hulk smash banannas')
            .METADATA({ author: 'An Avenger' }),
        ],
      });

      expect(sequence.toEDSLString()).toEqual(
        'export default () =>\n' +
          '  Sequence.new({\n' +
          "    seqId: 'Immediate',\n" +
          '    metadata: {},\n' +
          '    immediate_commands: [\n' +
          "      SMASH_BANANA('AGRESSIVE')\n" +
          "      .DESCRIPTION('Hulk smash banannas')\n" +
          '      .METADATA({\n' +
          "        author: 'An Avenger',\n" +
          '      }),\n' +
          '    ],\n' +
          '  });',
      );
    });
  });
});
