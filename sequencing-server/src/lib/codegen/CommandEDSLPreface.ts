/** START Preface */

export enum TimingTypes {
  ABSOLUTE = 'ABSOLUTE',
  COMMAND_RELATIVE = 'COMMAND_RELATIVE',
  EPOCH_RELATIVE = 'EPOCH_RELATIVE',
  COMMAND_COMPLETE = 'COMMAND_COMPLETE',
}

// @ts-ignore : 'Args' found in JSON Spec
export type CommandOptions<A extends Args[] | { [argName: string]: any } = [] | {}> = {
  stem: string;
  arguments: A;
  // @ts-ignore : 'Metadata' found in JSON Spec
  metadata?: Metadata | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  description?: Description | undefined;
  // @ts-ignore : 'Model' found in JSON Spec
  models?: Model[] | undefined;
} & (
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

export type Arrayable<T> = T | Arrayable<T>[];

export interface SequenceOptions {
  seqId: string;
  // @ts-ignore : 'Metadata' found in JSON Spec
  metadata: Metadata;

  // @ts-ignore : 'VariableDeclaration' found in JSON Spec
  locals?: [VariableDeclaration, ...VariableDeclaration[]];
  // @ts-ignore : 'VariableDeclaration' found in JSON Spec
  parameters?: [VariableDeclaration, ...VariableDeclaration[]];
  // @ts-ignore : 'Step' found in JSON Spec
  steps?: Step[];
  // @ts-ignore : 'Request' found in JSON Spec
  requests?: Request[];
  // @ts-ignore : 'ImmediateCommand' found in JSON Spec
  immediate_commands?: ImmediateCommand[];
  // @ts-ignore : 'HardwareCommand' found in JSON Spec
  hardware_commands?: HardwareCommand[];
}

declare global {
  // @ts-ignore : 'Args' found in JSON Spec
  class CommandStem<A extends Args[] | { [argName: string]: any } = [] | {}> implements Command {
    // @ts-ignore : 'Args' found in JSON Spec
    args: Args;
    stem: string;
    // @ts-ignore : 'TIME' found in JSON Spec
    time: Time;
    type: 'command';

    public static new<A extends any[] | { [argName: string]: any }>(opts: CommandOptions<A>): CommandStem<A>;

    // @ts-ignore : 'Command' found in JSON Spec
    public toSeqJson(): Command;

    // @ts-ignore : 'Model' found in JSON Spec
    public MODELS(models: Model[]): CommandStem<A>;
    // @ts-ignore : 'Model' found in JSON Spec
    public GET_MODELS(): Model[] | undefined;

    // @ts-ignore : 'Metadata' found in JSON Spec
    public METADATA(metadata: Metadata): CommandStem<A>;
    // @ts-ignore : 'Metadata' found in JSON Spec
    public GET_METADATA(): Metadata | undefined;

    // @ts-ignore : 'Description' found in JSON Spec
    public DESCRIPTION(description: Description): CommandStem<A>;
    // @ts-ignore : 'Description' found in JSON Spec
    public GET_DESCRIPTION(): Description | undefined;
  }
  // @ts-ignore : 'SeqJson' found in JSON Spec
  class Sequence implements SeqJson {
    public readonly id: string;
    // @ts-ignore : 'Metadata' found in JSON Spec
    public readonly metadata: Metadata;

    // @ts-ignore : 'VariableDeclaration' found in JSON Spec
    public readonly locals?: [VariableDeclaration, ...VariableDeclaration[]];
    // @ts-ignore : 'VariableDeclaration' found in JSON Spec
    public readonly parameters?: [VariableDeclaration, ...VariableDeclaration[]];
    // @ts-ignore : 'Step' found in JSON Spec
    public readonly steps?: Step[];
    // @ts-ignore : 'Request' found in JSON Spec
    public readonly requests?: Request[];
    // @ts-ignore : 'ImmediateCommand' found in JSON Spec
    public readonly immediate_commands?: ImmediateCommand[];
    // @ts-ignore : 'HardwareCommand' found in JSON Spec
    public readonly hardware_commands?: HardwareCommand[];
    [k: string]: unknown;

    public static new(
      opts:
        | {
            seqId: string;
            // @ts-ignore : 'VariableDeclaration' found in JSON Spec
            locals?: [VariableDeclaration, ...VariableDeclaration[]];
            // @ts-ignore : 'Metadata' found in JSON Spec
            metadata: Metadata;
            // @ts-ignore : 'VariableDeclaration' found in JSON Spec
            parameters?: [VariableDeclaration, ...VariableDeclaration[]];
            // @ts-ignore : 'Step' found in JSON Spec
            steps?: Step[];
            // @ts-ignore : 'Request' found in JSON Spec
            requests?: Request[];
            // @ts-ignore : 'ImmediateCommand' found in JSON Spec
            immediate_commands?: ImmediateCommand[];
            // @ts-ignore : 'HardwareCommand' found in JSON Spec
            hardware_commands?: HardwareCommand[];
          }
        // @ts-ignore : 'SeqJson' found in JSON Spec
        | SeqJson,
    ): Sequence;

    // @ts-ignore : 'SeqJson' found in JSON Spec
    public toSeqJson(): SeqJson;
  }

  type Context = {};
  type ExpansionReturn = Arrayable<CommandStem>;

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
  type FixedString = string;
  type F<BitLength extends 32 | 64> = number;
  type F32 = F<32>;
  type F64 = F<64>;

  // @ts-ignore : 'Commands' found in generated code
  function A(...args: [TemplateStringsArray, ...string[]]): typeof Commands;
  // @ts-ignore : 'Commands' found in generated code
  function A(absoluteTime: Temporal.Instant): typeof Commands;
  // @ts-ignore : 'Commands' found in generated code
  function A(timeDOYString: string): typeof Commands;

  // @ts-ignore : 'Commands' found in generated code
  function R(...args: [TemplateStringsArray, ...string[]]): typeof Commands;
  // @ts-ignore : 'Commands' found in generated code
  function R(duration: Temporal.Duration): typeof Commands;
  // @ts-ignore : 'Commands' found in generated code
  function R(timeHMSString: string): typeof Commands;

  // @ts-ignore : 'Commands' found in generated code
  function E(...args: [TemplateStringsArray, ...string[]]): typeof Commands;
  // @ts-ignore : 'Commands' found in generated code
  function E(duration: Temporal.Duration): typeof Commands;
  // @ts-ignore : 'Commands' found in generated code
  function E(timeHMSString: string): typeof Commands;

  // @ts-ignore : 'Commands' found in generated code
  const C: typeof Commands;
}

// @ts-ignore : 'Args' found in JSON Spec
export class CommandStem<A extends Args[] | { [argName: string]: any } = [] | {}> implements Command {
  public readonly arguments: A;
  public readonly absoluteTime: Temporal.Instant | null = null;
  public readonly epochTime: Temporal.Duration | null = null;
  public readonly relativeTime: Temporal.Duration | null = null;

  public readonly stem: string;
  // @ts-ignore : 'Args' found in JSON Spec
  public readonly args!: Args;
  // @ts-ignore : 'Time' found in JSON Spec
  public readonly time!: Time;
  // @ts-ignore : 'Model' found in JSON Spec
  private readonly _models?: Model[] | undefined;
  // @ts-ignore : 'Metadata' found in JSON Spec
  private readonly _metadata?: Metadata | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  private readonly _description?: Description | undefined;
  public readonly type: 'command' = 'command';

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
    this._metadata = opts.metadata;
    this._description = opts.description;
    this._models = opts.models;
  }

  public static new<A extends any[] | { [argName: string]: any }>(opts: CommandOptions<A>): CommandStem<A> {
    if ('absoluteTime' in opts) {
      return new CommandStem<A>({
        ...opts,
        absoluteTime: opts.absoluteTime,
      });
    } else if ('epochTime' in opts) {
      return new CommandStem<A>({
        ...opts,
        epochTime: opts.epochTime,
      });
    } else if ('relativeTime' in opts) {
      return new CommandStem<A>({
        ...opts,
        relativeTime: opts.relativeTime,
      });
    } else {
      return new CommandStem<A>(opts);
    }
  }

  // @ts-ignore : 'Model' found in JSON Spec
  public MODELS(models: Model[]): CommandStem {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      models: models,
      metadata: this._metadata,
      description: this._description,
      ...(this.absoluteTime && { absoluteTime: this.absoluteTime }),
      ...(this.epochTime && { epochTime: this.epochTime }),
      ...(this.relativeTime && { relativeTime: this.relativeTime }),
    });
  }

  // @ts-ignore : 'Model' found in JSON Spec
  public GET_MODELS(): Model[] | undefined {
    return this._models;
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public METADATA(metadata: Metadata): CommandStem {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      models: this._models,
      metadata: metadata,
      description: this._description,
      ...(this.absoluteTime && { absoluteTime: this.absoluteTime }),
      ...(this.epochTime && { epochTime: this.epochTime }),
      ...(this.relativeTime && { relativeTime: this.relativeTime }),
    });
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public GET_METADATA(): Metadata | undefined {
    return this._metadata;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public DESCRIPTION(description: Description): CommandStem {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      models: this._models,
      metadata: this._metadata,
      description: description,
      ...(this.absoluteTime && { absoluteTime: this.absoluteTime }),
      ...(this.epochTime && { epochTime: this.epochTime }),
      ...(this.relativeTime && { relativeTime: this.relativeTime }),
    });
  }
  // @ts-ignore : 'Description' found in JSON Spec
  public GET_DESCRIPTION(): Description | undefined {
    return this._description;
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public toSeqJson(): Command {
    return {
      args: CommandStem.convertArgsToInterfaces(this.arguments),
      stem: this.stem,
      time:
        this.absoluteTime !== null
          ? { type: TimingTypes.ABSOLUTE, tag: instantToDoy(this.absoluteTime) }
          : this.epochTime !== null
          ? { type: TimingTypes.EPOCH_RELATIVE, tag: durationToHms(this.epochTime) }
          : this.relativeTime !== null
          ? { type: TimingTypes.COMMAND_RELATIVE, tag: durationToHms(this.relativeTime) }
          : { type: TimingTypes.COMMAND_COMPLETE },
      type: this.type,
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { models: this._models } : {}),
      ...(this._description ? { description: this._description } : {}),
    };
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public static fromSeqJson(json: Command): CommandStem {
    const timeValue =
      json.time.type === TimingTypes.ABSOLUTE
        ? { absoluteTime: doyToInstant(json.time.tag as DOY_STRING) }
        : json.time.type === TimingTypes.COMMAND_RELATIVE
        ? { relativeTime: hmsToDuration(json.time.tag as HMS_STRING) }
        : json.time.type === TimingTypes.EPOCH_RELATIVE
        ? { epochTime: hmsToDuration(json.time.tag as HMS_STRING) }
        : {};

    return CommandStem.new({
      stem: json.stem,
      arguments: CommandStem.convertInterfacesToArgs(json.args),
      metadata: json.metadata,
      models: json.models,
      description: json.description,
      ...timeValue,
    });
  }

  public absoluteTiming(absoluteTime: Temporal.Instant): CommandStem<A> {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      absoluteTime: absoluteTime,
    });
  }

  public epochTiming(epochTime: Temporal.Duration): CommandStem<A> {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      epochTime: epochTime,
    });
  }

  public relativeTiming(relativeTime: Temporal.Duration): CommandStem<A> {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      relativeTime: relativeTime,
    });
  }

  public toEDSLString(): string {
    const timeString = this.absoluteTime
      ? `A\`${instantToDoy(this.absoluteTime)}\``
      : this.epochTime
      ? `E\`${durationToHms(this.epochTime)}\``
      : this.relativeTime
      ? `R\`${durationToHms(this.relativeTime)}\``
      : 'C';

    const argsString =
      Object.keys(this.arguments).length === 0 ? '' : `(${CommandStem.argumentsToString(this.arguments)})`;

    const metadata =
      this._metadata && Object.keys(this._metadata).length !== 0
        ? `\n.METADATA(${objectToString(this._metadata)})`
        : '';
    const description =
      this._description && this._description.length !== 0 ? `\n.DESCRIPTION('${this._description}')` : '';
    const models =
      this._models && Object.keys(this._models).length !== 0
        ? `\n.MODELS([\n${this._models.map(m => indent(objectToString(m))).join(',\n')}\n])`
        : '';
    return `${timeString}.${this.stem}${argsString}${description}${metadata}${models}`;
  }

  // @ts-ignore : 'Args' found in JSON Spec
  private static argumentsToString<A extends Args[] | { [argName: string]: any } = [] | {}>(args: A): string {
    if (Array.isArray(args)) {
      const argStrings = args.map(arg => {
        if (typeof arg === 'string') {
          return `'${arg}'`;
        }
        return arg.toString();
      });

      return argStrings.join(', ');
    } else {
      return objectToString(args);
    }
  }

  // This function takes an array of Args interfaces and converts it into an object or an array depending on whether
  // the repeat argument is true or false. The interfaces array contains objects matching the ARGS interface.
  // Depending on the type property of each object, a corresponding object with the name and value properties is created
  // and added to the output. If an object's type property is "repeat", then the value property is recursively passed
  // through the function with repeat set to true.Additionally, the function includes a validation function that prevents
  // remote property injection attacks.
  // @ts-ignore : 'Args' found in JSON Spec
  private static convertInterfacesToArgs(interfaces: Args, repeat = false): {} | [] {
    const args = interfaces.length === 0 ? [] : !repeat ? {} : [];

    //use to prevent a Remote property injection attack
    const validate = (input: string): boolean => {
      const pattern = /^[a-zA-Z0-9_-]+$/;
      const isValid = pattern.test(input);
      return isValid;
    };

    const convertedArgs = interfaces.map(
      (
        // @ts-ignore : found in JSON Spec
        arg: StringArgument | NumberArgument | BooleanArgument | SymbolArgument | HexArgument | RepeatArgument,
      ) => {
        // @ts-ignore : 'RepeatArgument' found in JSON Spec
        if (arg.type === 'repeat') {
          if (validate(arg.name)) {
            // @ts-ignore : 'RepeatArgument' found in JSON Spec
            return { [arg.name]: this.convertInterfacesToArgs(arg.value, true) };
          }
          return { repeat_error: 'Remote property injection detected...' };
        } else if (arg.type === 'symbol') {
          if (validate(arg.name)) {
            // @ts-ignore : 'SymbolArgument' found in JSON Spec
            return { [arg.name]: { symbol: arg.value } };
          }
          return { symbol_error: 'Remote property injection detected...' };
          // @ts-ignore : 'HexArgument' found in JSON Spec
        } else if (arg.type === 'hex') {
          if (validate(arg.name)) {
            // @ts-ignore : 'HexArgument' found in JSON Spec
            return { [arg.name]: { hex: arg.value } };
          }
          return { hex_error: 'Remote property injection detected...' };
        } else {
          if (validate(arg.name)) {
            return { [arg.name]: arg.value };
          }
          return { error: 'Remote property injection detected...' };
        }
      },
    );

    if (repeat) {
      let obj = {};
      for (const i in convertedArgs) {
        for (const key in convertedArgs[i]) {
          if (key in obj) {
            (args as any[]).push(obj);
            obj = {};
          }
          Object.assign(obj, convertedArgs[i]);
        }
      }
      (args as any[]).push(obj);
    } else {
      for (const i in convertedArgs) {
        Object.assign(args, convertedArgs[i]);
      }
    }

    return args;
  }

  //The function takes an object of arguments and converts them into the Args type. It does this by looping through the
  // values and pushing a new argument type to the result array depending on the type of the value.
  // If the value is an array, it will create a RepeatArgument type and recursively call on the values of the array.
  // the function returns the result array of argument types -
  // StringArgument, NumberArgument, BooleanArgument, SymbolArgument, HexArgument, and RepeatArgument.
  // @ts-ignore : 'Args' found in JSON Spec
  private static convertArgsToInterfaces(args: { [argName: string]: any }): Args {
    // @ts-ignore : 'Args' found in JSON Spec
    let result: Args = [];
    if (args['length'] === 0) {
      return result;
    }

    const values = Array.isArray(args) ? args[0] : args;

    for (let key in values) {
      let value = values[key];
      if (Array.isArray(value)) {
        // @ts-ignore : 'RepeatArgument' found in JSON Spec
        let repeatArg: RepeatArgument = {
          value: value
            .map(arg => {
              return this.convertArgsToInterfaces(arg);
            })
            .flat(),
          type: 'repeat',
          name: key,
        };
        result.push(repeatArg);
      } else {
        switch (typeof value) {
          case 'string':
            result.push({ type: 'string', value: value, name: key });
            break;
          case 'number':
            result.push({ type: 'number', value: value, name: key });
            break;
          case 'boolean':
            result.push({ type: 'boolean', value: value, name: key });
            break;
          default:
            if (value instanceof Object && value.symbol && value.symbol === 'string') {
              result.push({ type: 'symbol', value: value, name: key });
            } else if (
              value instanceof Object &&
              value.hex &&
              value.hex === 'string' &&
              new RegExp('^0x([0-9A-F])+$').test(value.hex)
            ) {
              result.push({ type: 'hex', value: value, name: key });
            }
            break;
        }
      }
    }
    return result;
  }
}

// @ts-ignore : 'SeqJson' found in JSON Spec
export class Sequence implements SeqJson {
  public readonly id: string;
  // @ts-ignore : 'Metadata' found in JSON Spec
  public readonly metadata: Metadata;

  // @ts-ignore : 'VariableDeclaration' found in JSON Spec
  public readonly locals?: [VariableDeclaration, ...VariableDeclaration[]];
  // @ts-ignore : 'VariableDeclaration' found in JSON Spec
  public readonly parameters?: [VariableDeclaration, ...VariableDeclaration[]];
  // @ts-ignore : 'Step' found in JSON Spec
  public readonly steps?: Step[];
  // @ts-ignore : 'Request' found in JSON Spec
  public readonly requests?: Request[];
  // @ts-ignore : 'ImmediateCommand' found in JSON Spec
  public readonly immediate_commands?: ImmediateCommand[];
  // @ts-ignore : 'HardwareCommand' found in JSON Spec
  public readonly hardware_commands?: HardwareCommand[];
  [k: string]: unknown;

  // @ts-ignore : 'SeqJson' found in JSON Spec
  private constructor(opts: SequenceOptions | SeqJson) {
    if ('id' in opts) {
      this.id = opts.id;
    } else {
      this.id = opts.seqId;
    }
    this.metadata = opts.metadata;

    this.locals = opts.locals ?? undefined;
    this.parameters = opts.parameters ?? undefined;
    this.steps = opts.steps ?? undefined;
    this.requests = opts.requests ?? undefined;
    this.immediate_commands = opts.immediate_commands ?? undefined;
    this.hardware_commands = opts.hardware_commands ?? undefined;
  }
  public static new(opts: SequenceOptions): Sequence {
    return new Sequence(opts);
  }

  // @ts-ignore : 'SeqJson' found in JSON Spec
  public toSeqJson(): SeqJson {
    return {
      id: this.id,
      metadata: this.metadata,
      ...(this.steps
        ? {
            steps: this.steps.map(step => {
              if (step instanceof CommandStem) return step.toSeqJson();
              return step;
            }),
          }
        : {}),
    };
  }

  public toEDSLString(): string {
    const commandsString =
      this.steps && this.steps.length > 0
        ? '\n' +
          indent(
            this.steps
              .map(step => {
                return (step as CommandStem).toEDSLString() + ',';
              })
              .join('\n'),
            1,
          )
        : '';

    return `export default () =>
  Sequence.new({
	  seqId: '${this.id}',
	  metadata: ${JSON.stringify(this.metadata)},${
      commandsString.length > 0 ? `\n${indent(`steps: [${commandsString}`, 2)}\n${indent('],', 2)}` : ''
    }
  });`;
  }

  // @ts-ignore : 'Args' found in JSON Spec
  public static fromSeqJson(json: SeqJson): Sequence {
    return Sequence.new({
      seqId: json.id,
      metadata: json.metadata,
      // @ts-ignore : 'Step' found in JSON Spec
      ...(json.steps ? { steps: json.steps.map((c: Step) => CommandStem.fromSeqJson(c as CommandStem)) } : {}),
      ...(json.locals ? { locals: json.locals } : {}),
      ...(json.parameters ? { parameters: json.parameters } : {}),
      ...(json.requests ? { requests: json.requests } : {}),
      ...(json.immediate_commands ? { immediate_commands: json.immediate_commands } : {}),
      ...(json.hardware_commands ? { hardware_commands: json.hardware_commands } : {}),
    });
  }
}

/** Time utilities */

export type DOY_STRING = string & { __brand: 'DOY_STRING' };
export type HMS_STRING = string & { __brand: 'HMS_STRING' };

const DOY_REGEX = /(\d{4})-(\d{3})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}))?/;
const HMS_REGEX = /(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}))?/;

/** YYYY-DOYTHH:MM:SS.sss */
export function instantToDoy(time: Temporal.Instant): DOY_STRING {
  const utcZonedDate = time.toZonedDateTimeISO('UTC');
  const YYYY = formatNumber(utcZonedDate.year, 4);
  const DOY = formatNumber(utcZonedDate.dayOfYear, 3);
  const HH = formatNumber(utcZonedDate.hour, 2);
  const MM = formatNumber(utcZonedDate.minute, 2);
  const SS = formatNumber(utcZonedDate.second, 2);
  const sss = formatNumber(utcZonedDate.millisecond, 3);
  return `${YYYY}-${DOY}T${HH}:${MM}:${SS}.${sss}` as DOY_STRING;
}

export function doyToInstant(doy: DOY_STRING): Temporal.Instant {
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

  //use to convert doy to month and day
  const doyDate = new Date(parseInt(year, 10), 0, parseInt(doyStr, 10));
  // convert to UTC Date
  const utcDoyDate = new Date(
    Date.UTC(
      doyDate.getUTCFullYear(),
      doyDate.getUTCMonth(),
      doyDate.getUTCDate(),
      doyDate.getUTCHours(),
      doyDate.getUTCMinutes(),
      doyDate.getUTCSeconds(),
      doyDate.getUTCMilliseconds(),
    ),
  );

  return Temporal.ZonedDateTime.from({
    year: parseInt(year, 10),
    month: utcDoyDate.getUTCMonth() + 1,
    day: utcDoyDate.getUTCDate(),
    hour: parseInt(hour, 10),
    minute: parseInt(minute, 10),
    second: parseInt(second, 10),
    millisecond: parseInt(millisecond ?? '0', 10),
    timeZone: 'UTC',
  }).toInstant();
}

/** HH:MM:SS.sss */
export function durationToHms(time: Temporal.Duration): HMS_STRING {
  const HH = formatNumber(time.hours, 2);
  const MM = formatNumber(time.minutes, 2);
  const SS = formatNumber(time.seconds, 2);
  const sss = formatNumber(time.milliseconds, 3);

  return `${HH}:${MM}:${SS}.${sss}` as HMS_STRING;
}

export function hmsToDuration(hms: HMS_STRING): Temporal.Duration {
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

function formatNumber(number: number, size: number): string {
  return number.toString().padStart(size, '0');
}

// @ts-ignore : Used in generated code
function A(...args: [TemplateStringsArray, ...string[]] | [Temporal.Instant] | [string]): typeof Commands {
  let time: Temporal.Instant;
  if (Array.isArray(args[0])) {
    time = doyToInstant(String.raw(...(args as [TemplateStringsArray, ...string[]])) as DOY_STRING);
  } else if (typeof args[0] === 'string') {
    time = doyToInstant(args[0] as DOY_STRING);
  } else {
    time = args[0] as Temporal.Instant;
  }

  return commandsWithTimeValue(time, TimingTypes.ABSOLUTE);
}

// @ts-ignore : Used in generated code
function R(...args: [TemplateStringsArray, ...string[]] | [Temporal.Duration] | [string]): typeof Commands {
  let duration: Temporal.Duration;
  if (Array.isArray(args[0])) {
    duration = hmsToDuration(String.raw(...(args as [TemplateStringsArray, ...string[]])) as HMS_STRING);
  } else if (typeof args[0] === 'string') {
    duration = hmsToDuration(args[0] as HMS_STRING);
  } else {
    duration = args[0] as Temporal.Duration;
  }

  return commandsWithTimeValue(duration, TimingTypes.COMMAND_RELATIVE);
}

// @ts-ignore : Used in generated code
function E(...args: [TemplateStringsArray, ...string[]] | [Temporal.Duration] | [string]): typeof Commands {
  let duration: Temporal.Duration;
  if (Array.isArray(args[0])) {
    duration = hmsToDuration(String.raw(...(args as [TemplateStringsArray, ...string[]])) as HMS_STRING);
  } else if (typeof args[0] === 'string') {
    duration = hmsToDuration(args[0] as HMS_STRING);
  } else {
    duration = args[0] as Temporal.Duration;
  }
  return commandsWithTimeValue(duration, TimingTypes.EPOCH_RELATIVE);
}

function commandsWithTimeValue<T extends TimingTypes>(
  timeValue: Temporal.Instant | Temporal.Duration,
  timeType: T,
  // @ts-ignore : 'Commands' found in generated code
): typeof Commands {
  // @ts-ignore : 'Commands' found in generated code
  return Object.keys(Commands).reduce((accum, key) => {
    // @ts-ignore : 'Commands' found in generated code
    const command = Commands[key as keyof Commands];
    if (typeof command === 'function') {
      if (timeType === TimingTypes.ABSOLUTE) {
        accum[key] = (...args: Parameters<typeof command>): typeof command => {
          return command(...args).absoluteTiming(timeValue);
        };
      } else if (timeType === TimingTypes.COMMAND_RELATIVE) {
        accum[key] = (...args: Parameters<typeof command>): typeof command => {
          return command(...args).relativeTiming(timeValue);
        };
      } else {
        accum[key] = (...args: Parameters<typeof command>): typeof command => {
          return command(...args).epochTiming(timeValue);
        };
      }
    } else {
      if (timeType === TimingTypes.ABSOLUTE) {
        accum[key] = command.absoluteTiming(timeValue);
      } else if (timeType === TimingTypes.COMMAND_RELATIVE) {
        accum[key] = command.relativeTiming(timeValue);
      } else {
        accum[key] = command.epochTiming(timeValue);
      }
    }
    return accum;
    // @ts-ignore : 'Commands' found in generated code
  }, {} as typeof Commands);
}

function indent(text: string, numTimes: number = 1, char: string = '  '): string {
  return text
    .split('\n')
    .map(line => char.repeat(numTimes) + line)
    .join('\n');
}

// This method takes an object and converts it to a string representation, with each key-value pair on a new line
// and nested objects/arrays indented. The indentLevel parameter specifies the initial indentation level,
// used to prettify the generated eDSL from SeqJSON
function objectToString(obj: any, indentLevel: number = 1): string {
  let output = '';

  const print = (obj: any) => {
    Object.keys(obj).forEach(key => {
      const value = obj[key];

      if (Array.isArray(value)) {
        output += indent(`${key}: [`, indentLevel) + '\n';
        indentLevel++;
        value.forEach((item: any) => {
          output += indent(`{`, indentLevel) + '\n';
          indentLevel++;
          print(item);
          indentLevel--;
          output += indent(`},`, indentLevel) + '\n';
        });
        indentLevel--;
        output += indent(`],`, indentLevel) + '\n';
      } else if (typeof value === 'object') {
        output += indent(`${key}:{`, indentLevel) + '\n';
        indentLevel++;
        print(value);
        indentLevel--;
        output += indent(`},`, indentLevel) + '\n';
      } else {
        output += indent(`${key}: ${typeof value === 'string' ? `'${value}'` : value},`, indentLevel) + '\n';
      }
    });
  };

  output += '{\n';
  print(obj);
  output += `}`;

  return output;
}

/** END Preface */
