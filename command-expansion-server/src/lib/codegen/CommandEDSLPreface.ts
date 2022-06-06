/** START Preface */
export type DOY_STRING = string & { __brand: 'DOY_STRING' };
export type HMS_STRING = string & { __brand: 'HMS_STRING' };

export enum TimingTypes {
  ABSOLUTE = 'ABSOLUTE',
  COMMAND_RELATIVE = 'COMMAND_RELATIVE',
  EPOCH_RELATIVE = 'EPOCH_RELATIVE',
  COMMAND_COMPLETE = 'COMMAND_COMPLETE',
}

type SeqJsonTimeType =
  | {
      type: TimingTypes.ABSOLUTE;
      tag: DOY_STRING;
    }
  | {
      type: TimingTypes.COMMAND_RELATIVE;
      tag: HMS_STRING;
    }
  | {
      type: TimingTypes.EPOCH_RELATIVE;
      tag: HMS_STRING;
    }
  | {
      type: TimingTypes.COMMAND_COMPLETE;
    };

export type CommandOptions<
  A extends ArgType[] | { [argName: string]: any } = [] | {},
  M extends Record<string, any> = Record<string, any>,
> = { stem: string; arguments: A; metadata?: M } & (
  | {
      absoluteTime: Temporal.Instant;
    }
  | {
      epochTime: Temporal.Duration;
    }
  | {
      relativeTime: Temporal.Duration;
    }
  // CommandComplete
  | {}
);

export interface CommandSeqJson<A extends ArgType[] = ArgType[]> {
  args: A;
  stem: string;
  time: SeqJsonTimeType;
  type: 'command';
  metadata: Record<string, unknown>;
}

export type ArgType = boolean | string | number;
export type Arrayable<T> = T | Arrayable<T>[];

export interface SequenceSeqJson {
  id: string;
  metadata: Record<string, any>;
  steps: CommandSeqJson[];
}

declare global {
  class Command<A extends ArgType[] | { [argName: string]: any } = [] | {}> {
    public static new<A extends any[] | { [argName: string]: any }>(opts: CommandOptions<A>): Command<A>;

    public toSeqJson(): CommandSeqJson;

    public absoluteTiming(absoluteTime: Temporal.Instant): Command<A>;

    public epochTiming(epochTime: Temporal.Duration): Command<A>;

    public relativeTiming(relativeTime: Temporal.Duration): Command<A>;
  }
  type Context = {};
  type ExpansionReturn = Arrayable<Command>;

  type U<BitLength extends 8 | 16 | 32 | 64> = number;
  type U8 = U<8>;
  type U16 = U<16>;
  type U32 = U<32>;
  type U64 = U<64>;
  type I<BitLength extends 8 | 16 | 32 | 64> = number;
  type I8 = I<8>;
  type I16 = I<16>;
  type I32 = I<32>;
  type I64 = I<64>;
  type VarString<PrefixBitLength extends number, MaxBitLength extends number> = string;
  type F<BitLength extends 32 | 64> = number;
  type F32 = F<32>;
  type F64 = F<64>;
}
/** END Preface */

const DOY_REGEX = /(\d{4})-(\d{3})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}))?/;
const HMS_REGEX = /(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}))?/;

export class Command<
  A extends ArgType[] | { [argName: string]: any } = [] | {},
  M extends Record<string, any> = Record<string, any>,
> {
  public readonly stem: string;
  public readonly metadata: M;
  public readonly arguments: A;
  public readonly absoluteTime: Temporal.Instant | null = null;
  public readonly epochTime: Temporal.Duration | null = null;
  public readonly relativeTime: Temporal.Duration | null = null;

  private constructor(opts: CommandOptions<A, M>) {
    this.stem = opts.stem;
    this.arguments = opts.arguments;
    this.metadata = opts.metadata ?? ({} as M);
    if ('absoluteTime' in opts) {
      this.absoluteTime = opts.absoluteTime;
    } else if ('epochTime' in opts) {
      this.epochTime = opts.epochTime;
    } else if ('relativeTime' in opts) {
      this.relativeTime = opts.relativeTime;
    }
  }

  public static new<A extends any[] | { [argName: string]: any }>(opts: CommandOptions<A>): Command<A> {
    if ('absoluteTime' in opts) {
      return new Command<A>({
        ...opts,
        absoluteTime: opts.absoluteTime,
      });
    } else if ('epochTime' in opts) {
      return new Command<A>({
        ...opts,
        epochTime: opts.epochTime,
      });
    } else if ('relativeTime' in opts) {
      return new Command<A>({
        ...opts,
        relativeTime: opts.relativeTime,
      });
    } else {
      return new Command<A>(opts);
    }
  }

  public toSeqJson(): CommandSeqJson {
    return {
      args:
        typeof this.arguments == 'object'
          ? this.serializeArguments(Object.values(this.arguments))
          : this.serializeArguments(this.arguments),
      stem: this.stem,
      time:
        this.absoluteTime !== null
          ? { type: TimingTypes.ABSOLUTE, tag: Command.instantToDoy(this.absoluteTime) }
          : this.epochTime !== null
          ? { type: TimingTypes.EPOCH_RELATIVE, tag: Command.durationToHms(this.epochTime) }
          : this.relativeTime !== null
          ? { type: TimingTypes.COMMAND_RELATIVE, tag: Command.durationToHms(this.relativeTime) }
          : { type: TimingTypes.COMMAND_COMPLETE },
      type: 'command',
      metadata: {},
    };
  }

  private serializeArguments(args: ArgType[]): ArgType[] {
    return args.map(arg => {
      if (typeof arg == 'boolean') {
        // Europa Clipper boolean values are 0 or 1
        return arg ? 1 : 0;
      }
      return arg;
    });
  }

  public static fromSeqJson<A extends ArgType[]>(json: CommandSeqJson<A>): Command<A> {
    const timeValue =
      json.time.type === TimingTypes.ABSOLUTE
        ? { absoluteTime: Command.doyToInstant(json.time.tag) }
        : json.time.type === TimingTypes.COMMAND_RELATIVE
        ? { relativeTime: Command.hmsToDuration(json.time.tag) }
        : json.time.type === TimingTypes.EPOCH_RELATIVE
        ? { epochTime: Command.hmsToDuration(json.time.tag) }
        : {};

    return Command.new<A>({
      stem: json.stem,
      arguments: json.args as A,
      metadata: json.metadata,
      ...timeValue,
    });
  }

  public absoluteTiming(absoluteTime: Temporal.Instant): Command<A> {
    return Command.new({
      stem: this.stem,
      arguments: this.arguments,
      absoluteTime: absoluteTime,
    });
  }

  public epochTiming(epochTime: Temporal.Duration): Command<A> {
    return Command.new({
      stem: this.stem,
      arguments: this.arguments,
      epochTime: epochTime,
    });
  }

  public relativeTiming(relativeTime: Temporal.Duration): Command<A> {
    return Command.new({
      stem: this.stem,
      arguments: this.arguments,
      relativeTime: relativeTime,
    });
  }

  /** YYYY-DOYTHH:MM:SS.sss */
  private static instantToDoy(time: Temporal.Instant): DOY_STRING {
    const utcZonedDate = time.toZonedDateTimeISO('UTC');
    const YYYY = this.formatNumber(utcZonedDate.year, 4);
    const DOY = this.formatNumber(utcZonedDate.dayOfYear, 3);
    const HH = this.formatNumber(utcZonedDate.hour, 2);
    const MM = this.formatNumber(utcZonedDate.minute, 2);
    const SS = this.formatNumber(utcZonedDate.second, 2);
    const sss = this.formatNumber(utcZonedDate.millisecond, 3);
    return `${YYYY}-${DOY}T${HH}:${MM}:${SS}.${sss}` as DOY_STRING;
  }

  private static doyToInstant(doy: DOY_STRING): Temporal.Instant {
    const match = doy.match(DOY_REGEX);
    if (match === null) {
      throw new Error(`Invalid DOY string: ${doy}`);
    }
    const [, year, doyStr, hour, minute, second, millisecond] = match as [
      unknown,
      string,
      string,
      string,
      string,
      string,
      string | undefined,
    ];
    return Temporal.ZonedDateTime.from({
      year: parseInt(year, 10),
      dayOfYear: parseInt(doyStr, 10),
      hour: parseInt(hour, 10),
      minute: parseInt(minute, 10),
      second: parseInt(second, 10),
      millisecond: parseInt(millisecond ?? '0', 10),
      timeZone: 'UTC',
    }).toInstant();
  }

  /** HH:MM:SS.sss */
  private static durationToHms(time: Temporal.Duration): HMS_STRING {
    const HH = this.formatNumber(time.hours, 2);
    const MM = this.formatNumber(time.minutes, 2);
    const SS = this.formatNumber(time.seconds, 2);
    const sss = this.formatNumber(time.milliseconds, 3);

    return `${HH}:${MM}:${SS}.${sss}` as HMS_STRING;
  }

  private static hmsToDuration(hms: HMS_STRING): Temporal.Duration {
    const match = hms.match(HMS_REGEX);
    if (match === null) {
      throw new Error(`Invalid HMS string: ${hms}`);
    }
    const [, hours, minutes, seconds, milliseconds] = match as [unknown, string, string, string, string | undefined];
    return Temporal.Duration.from({
      hours: parseInt(hours, 10),
      minutes: parseInt(minutes, 10),
      seconds: parseInt(seconds, 10),
      milliseconds: parseInt(milliseconds ?? '0', 10),
    });
  }

  private static formatNumber(number: number, size: number): string {
    return number.toString().padStart(size, '0');
  }
}

export interface SequenceOptions {
  seqId: string;
  metadata: Record<string, any>;
  commands: Command[];
}

export class Sequence {
  public readonly seqId: string;
  public readonly metadata: Record<string, any>;
  public readonly commands: Command[];

  private constructor(opts: SequenceOptions) {
    this.seqId = opts.seqId;
    this.metadata = opts.metadata;
    this.commands = opts.commands;
  }

  public static new(opts: SequenceOptions): Sequence {
    return new Sequence(opts);
  }

  public toSeqJson(): SequenceSeqJson {
    return {
      id: this.seqId,
      metadata: this.metadata,
      steps: this.commands.map(c => c.toSeqJson()),
    };
  }

  public static fromSeqJson(json: SequenceSeqJson): Sequence {
    return Sequence.new({
      seqId: json.id,
      metadata: json.metadata,
      commands: json.steps.map(c => Command.fromSeqJson(c)),
    });
  }
}

//helper functions
function orderCommandArguments(args: { [argName: string]: any }, order: string[]): any {
  return order.map(key => args[key]);
}

/** END Preface */
