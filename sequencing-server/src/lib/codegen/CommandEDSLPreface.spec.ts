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
      const sequence = Sequence.new({
        seqId: 'test',
        metadata: {},
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
        ],
      });

      expect(sequence.toEDSLString()).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test',
    metadata: {},
    steps: [
      A\`2020-001T00:00:00.000\`.TEST('string', 0, true),
      A\`2020-001T00:00:00.000\`.TEST({
        string: 'string',
        number: 0,
        boolean: true,
      })
      .METADATA({
        author: 'XXXXXXXXXXXXXXXXXXXXXXXXXX',
      }),
    ],
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
  });
});
