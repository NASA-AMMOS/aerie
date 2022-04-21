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

  public toSeqJson() {
    return {
      id: 'command',
      metadata: {},

      steps: [
        {
          stem: this.stem,
          time: {
            tag: this.absoluteTime
              ? this.absoluteTime.toString()
              : this.relativeTime
              ? this.relativeTime.toString()
              : this.epochTime
              ? this.epochTime.time.toString()
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
          // include epoch field if we have an epoch time
          ...(this.epochTime !== null ? { epoch: this.epochTime?.epochName } : {}),
          args: typeof this.arguments == 'object' ? Object.values(this.arguments) : this.arguments,
        },
      ],
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
