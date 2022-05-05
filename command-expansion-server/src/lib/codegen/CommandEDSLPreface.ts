/** START Preface */
export class Command<A extends ArgType[] | { [argName: string]: any } = [] | {}> {
  public readonly stem: string;
  public readonly arguments: A;
  public readonly absoluteTime: Temporal.Instant | null = null;
  public readonly epochTime: { epochName: string; time: Temporal.Duration } | null = null;
  public readonly relativeTime: Temporal.Duration | null = null;

  private constructor(opts: CommandOptions<A>) {
    this.stem = opts.stem;
    this.arguments = opts.arguments;
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

  // return  yyyy-doyThh:mm:ss.sss
  private instantToDoy(time: Temporal.Instant): string {
    const utcZonedDate = time.toZonedDateTimeISO('UTC');
    return `${utcZonedDate.year}-${this.format(utcZonedDate.dayOfYear, 3)}T${this.format(
      utcZonedDate.hour,
      2,
    )}:${this.format(utcZonedDate.minute, 2)}:${this.format(utcZonedDate.second, 2)}.${this.format(
      utcZonedDate.millisecond,
      3,
    )}`;
  }

  // return hh:mm:ss.sss
  private durationToHms(time: Temporal.Duration): string {
    return `${this.format(time.hours, 2)}:${this.format(time.minutes, 2)}:${this.format(time.seconds, 2)}.${this.format(
      time.milliseconds,
      3,
    )}`;
  }

  private format(number: number, size: number): string {
    return number.toString().padStart(size, '0');
  }

  public toSeqJson() {
    return {
      args: typeof this.arguments == 'object' ? Object.values(this.arguments) : this.arguments,
      stem: this.stem,
      time: {
        tag: this.absoluteTime
          ? this.instantToDoy(this.absoluteTime) // yyyy-doyThh:mm:ss.sss
          : this.relativeTime
          ? this.durationToHms(this.relativeTime) // hh:mm:ss.sss
          : this.epochTime
          ? this.durationToHms(this.epochTime.time) // hh:mm:ss.sss
          : '',
        type: this.absoluteTime
          ? 'ABSOLUTE'
          : this.relativeTime
          ? 'COMMAND_RELATIVE'
          : this.epochTime
          ? 'EPOCH_RELATIVE'
          : 'COMMAND_COMPLETE',
      },
      type: 'command',
      metadata: {},
    };
  }

  public absoluteTiming(absoluteTime: Temporal.Instant): Command<A> {
    return Command.new({
      stem: this.stem,
      arguments: this.arguments,
      absoluteTime: absoluteTime,
    });
  }

  public epochTiming(epochName: string, epochTime: Temporal.Duration): Command<A> {
    return Command.new({
      stem: this.stem,
      arguments: this.arguments,
      epochTime: { epochName, time: epochTime },
    });
  }

  public relativeTiming(relativeTime: Temporal.Duration): Command<A> {
    return Command.new({
      stem: this.stem,
      arguments: this.arguments,
      relativeTime: relativeTime,
    });
  }
}

declare global {
  export class Command<A extends ArgType[] | { [argName: string]: any } = [] | {}> {
    public readonly stem: string;
    public readonly arguments: A;
    public readonly absoluteTime: Temporal.Instant | null;
    public readonly epochTime: { epochName: string; time: Temporal.Duration } | null;
    public readonly relativeTime: Temporal.Duration | null;

    private constructor(opts: CommandOptions<A>);

    public static new<A extends any[] | { [argName: string]: any }>(opts: CommandOptions<A>): Command<A>;

    public toSeqJson(): any;

    public absoluteTiming(absoluteTime: Temporal.Instant): Command<A>;

    public epochTiming(epochName: string, epochTime: Temporal.Duration): Command<A>;

    public relativeTiming(relativeTime: Temporal.Duration): Command<A>;
  }
  type Context = {};
  type ArgType = boolean | string | number;
  type Arrayable<T> = T | Arrayable<T>[];
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
  type CommandOptions<A> = { stem: string; arguments: A } & (
    | {
        absoluteTime: Temporal.Instant;
      }
    | {
        epochTime: { epochName: string; time: Temporal.Duration };
      }
    | {
        relativeTime: Temporal.Duration;
      }
    // CommandComplete
    | {}
  );
}
/** END Preface */
