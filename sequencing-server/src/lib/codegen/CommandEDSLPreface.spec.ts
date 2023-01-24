import {
  CommandStem,
  doyToInstant,
  DOY_STRING,
  hmsToDuration,
  HMS_STRING,
  Sequence,
  TimingTypes,
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
      expect(command.metadata).toEqual({});
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
            metadata: {},
            args: ['string', 0, true],
            time: { type: TimingTypes.ABSOLUTE, tag: '2020-001T00:00:00.000' as DOY_STRING },
          },
        ],
      });

      expect(sequence).toBeInstanceOf(Sequence);
      expect(sequence.seqId).toBe('test00000');
      expect(sequence.metadata).toEqual({});
      expect(sequence.commands.length).toEqual(2);

      expect(sequence.commands[0]!).toBeInstanceOf(CommandStem);
      expect(sequence.commands[0]!.stem).toBe('test');
      expect(sequence.commands[0]!.metadata).toEqual({});
      expect(sequence.commands[0]!.arguments).toEqual([]);

      expect(sequence.commands[1]!).toBeInstanceOf(CommandStem);
      expect(sequence.commands[1]!.stem).toBe('test2');
      expect(sequence.commands[1]!.metadata).toEqual({});
      expect(sequence.commands[1]!.arguments).toEqual(['string', 0, true]);
    });
  });

  describe('toEDSLString()', () => {
    it('should convert with no commands', () => {
      const sequence = Sequence.new({
        seqId: 'test',
        metadata: {},
        commands: [],
      });

      expect(sequence.toEDSLString()).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test',
    metadata: {},
    commands: [
    ],
  });`);
    });

    it('should convert with commands', () => {
      const sequence = Sequence.new({
        seqId: 'test',
        metadata: {},
        commands: [
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
          }),
        ],
      });

      expect(sequence.toEDSLString()).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test',
    metadata: {},
    commands: [
      A\`2020-001T00:00:00.000\`.TEST('string', 0, true),
      A\`2020-001T00:00:00.000\`.TEST({
        string: 'string',
        number: 0,
        boolean: true,
      }),
    ],
  });`);
    });
  });
});
