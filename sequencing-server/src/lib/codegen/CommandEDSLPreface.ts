/** START Preface */

export enum TimingTypes {
  ABSOLUTE = 'ABSOLUTE',
  COMMAND_RELATIVE = 'COMMAND_RELATIVE',
  EPOCH_RELATIVE = 'EPOCH_RELATIVE',
  COMMAND_COMPLETE = 'COMMAND_COMPLETE',
}

export enum VariableType {
  FLOAT = 'FLOAT',
  INT = 'INT',
  STRING = 'STRING',
  UINT = 'UINT',
  ENUM = 'ENUM'
}

export type VariableOptions = {
  name: string;
  type: VariableType;
  enum_name?: string | undefined;
  allowable_values?: unknown[] | undefined;
  // @ts-ignore : 'VariableRange' found in JSON Spec
  allowable_ranges?: VariableRange[] | undefined;
  sc_name?: string | undefined;
};
// @ts-ignore : 'VariableDeclaration' found in JSON Spec
export interface INT<N extends string> extends VariableDeclaration {
  name: N;
  type: VariableType.INT;
  // @ts-ignore : 'VariableRange' found in JSON Spec
  allowable_ranges?: VariableRange[] | undefined;
  allowable_values?: unknown[] | undefined;
  sc_name?: string | undefined;
}

// @ts-ignore : 'VariableDeclaration' found in JSON Spec
export interface UINT<N extends string> extends VariableDeclaration {
  name: N;
  type: VariableType.UINT;
  // @ts-ignore : 'VariableRange' found in JSON Spec
  allowable_ranges?: VariableRange[] | undefined;
  allowable_values?: unknown[] | undefined;
  sc_name?: string | undefined;
}

// @ts-ignore : 'VariableDeclaration' found in JSON Spec
export interface FLOAT<N extends string> extends VariableDeclaration {
  name: N;
  type: VariableType.FLOAT;
  // @ts-ignore : 'VariableRange' found in JSON Spec
  allowable_ranges?: VariableRange[] | undefined;
  allowable_values?: unknown[] | undefined;
  sc_name?: string | undefined;
}

// @ts-ignore : 'VariableDeclaration' found in JSON Spec
export interface STRING<N extends string> extends VariableDeclaration {
  name: N;
  type: VariableType.STRING;
  allowable_values?: unknown[] | undefined;
  sc_name?: string | undefined;
}

// @ts-ignore : 'VariableDeclaration' found in JSON Spec
export interface ENUM<N extends string, E extends string> extends VariableDeclaration {
  name: N;
  enum_name: E;
  type: VariableType.ENUM;
  // @ts-ignore : 'VariableRange' found in JSON Spec
  allowable_ranges?: VariableRange[] | undefined;
  allowable_values?: unknown[] | undefined;
  sc_name?: string | undefined;
}

export type SequenceOptions = {
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
};

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

// @ts-ignore : 'Args' found in JSON Spec
export type ImmediateOptions<A extends Args[] | { [argName: string]: any } = [] | {}> = {
  stem: string;
  arguments: A;
  // @ts-ignore : 'Metadata' found in JSON Spec
  metadata?: Metadata | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  description?: Description | undefined;
};

export type HardwareOptions = {
  stem: string;
  // @ts-ignore : 'Description' found in JSON Spec
  description?: Description;
  // @ts-ignore : 'Metadata' found in JSON Spec
  metadata?: Metadata;
};

export type GroundOptions = {
  name: string;
  // @ts-ignore : 'Args' found in JSON Spec
  args?: Args;
  // @ts-ignore : 'Description' found in JSON Spec
  description?: Description;
  // @ts-ignore : 'Metadata' found in JSON Spec
  metadata?: Metadata;
  // @ts-ignore : 'Model' found in JSON Spec
  models?: Model[];
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

declare global {
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

    public static new<
        // @ts-ignore : 'VariableDeclaration' found in JSON Spec
        const Locals extends ReadonlyArray<VariableDeclaration>,
        // @ts-ignore : 'VariableDeclaration' found in JSON Spec
        const Parameters extends ReadonlyArray<VariableDeclaration>,
    >(
        opts:
            | {
          seqId: string;
          // @ts-ignore : 'Metadata' found in JSON Spec
          metadata: Metadata;
          locals?: Locals;
          parameters?: Parameters;
          steps: // @ts-ignore : 'Step' found in JSON Spec
              | Step[]
              | ((opts: {
            /*Fancy way to map our array of objects to an object with name as key and type as value
             * this needs to be inlined instead of extracted to a helper type alias so that monaco won't hide the underlying type behind the alias
             */
            locals: { [Index in Locals[number] as Index['name']]: Index['type'] };
            parameters: { [Index in Parameters[number] as Index['name']]: Index['type'] };
            // @ts-ignore : 'Step' found in JSON Spec
          }) => Step[]);
          // @ts-ignore : 'ImmediateCommand' found in JSON Spec
          immediate_commands?: ImmediateCommand[];
          // @ts-ignore : 'HardwareCommand' found in JSON Spec
          hardware_commands?: HardwareCommand[];
          // @ts-ignore : 'Request' found in JSON Spec
          requests?: Request[] | ((opts : {
            locals: { [Index in Locals[number] as Index['name']]: Index['type'] };
            parameters: { [Index in Parameters[number] as Index['name']]: Index['type'] };
            // @ts-ignore : 'Step' found in JSON Spec
          }) => Request[]);
        }
            // @ts-ignore : 'SeqJson' found in JSON Spec
            | SeqJson,
    ): Sequence;

    // @ts-ignore : 'SeqJson' found in JSON Spec
    public toSeqJson(): SeqJson;
  }

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

  // @ts-ignore : 'ARGS' found in JSON Spec
  class ImmediateStem<A extends Args[] | { [argName: string]: any } = [] | {}> implements ImmediateCommand {
    // @ts-ignore : 'Args' found in JSON Spec
    args: Args;
    stem: string;

    public static new<A extends any[] | { [argName: string]: any }>(opts: ImmediateOptions<A>): ImmediateStem<A>;

    // @ts-ignore : 'Command' found in JSON Spec
    public toSeqJson(): ImmediateCommand;

    // @ts-ignore : 'Metadata' found in JSON Spec
    public METADATA(metadata: Metadata): ImmediateStem<A>;
    // @ts-ignore : 'Metadata' found in JSON Spec
    public GET_METADATA(): Metadata | undefined;

    // @ts-ignore : 'Description' found in JSON Spec
    public DESCRIPTION(description: Description): ImmediateStem<A>;
    // @ts-ignore : 'Description' found in JSON Spec
    public GET_DESCRIPTION(): Description | undefined;
  }

  // @ts-ignore : 'HardwareCommand' found in JSON Spec
  class HardwareStem implements HardwareCommand {
    stem: string;

    public static new(opts: HardwareOptions): HardwareStem;

    // @ts-ignore : 'Command' found in JSON Spec
    public toSeqJson(): HardwareCommand;

    // @ts-ignore : 'Metadata' found in JSON Spec
    public METADATA(metadata: Metadata): HardwareStem;
    // @ts-ignore : 'Metadata' found in JSON Spec
    public GET_METADATA(): Metadata | undefined;

    // @ts-ignore : 'Description' found in JSON Spec
    public DESCRIPTION(description: Description): HardwareStem;
    // @ts-ignore : 'Description' found in JSON Spec
    public GET_DESCRIPTION(): Description | undefined;
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

  function INT<const N extends string>(name: N): INT<N>;
  function INT<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): INT<N>;
  function INT<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals?: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): INT<N>;

  function UINT<const N extends string>(name: N): UINT<N>;
  function UINT<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): UINT<N>;
  function UINT<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals?: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): UINT<N>;

  function FLOAT<const N extends string>(name: N): FLOAT<N>;
  function FLOAT<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): FLOAT<N>;
  function FLOAT<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals?: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): FLOAT<N>;

  // @ts-ignore : 'VariableRange' found in JSON Spec
  function STRING<const N extends string>(name: N): STRING<N>;
  // @ts-ignore : 'VariableRange' found in JSON Spec
  function STRING<const N extends string>(
      name: N,
      optionals: { allowable_values?: unknown[]; sc_name?: string },
  ): STRING<N>;
  function STRING<const N extends string>(
      name: N,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals?: { allowable_values?: unknown[]; sc_name?: string },
  ): STRING<N>;

  function ENUM<const N extends string, const E extends string>(name: N, enum_name: E): ENUM<N, E>;
  function ENUM<const N extends string, const E extends string>(
      name: N,
      enum_name: E,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): ENUM<N, E>;
  function ENUM<const N extends string, const E extends string>(
      name: N,
      enum_name: E,
      // @ts-ignore : 'VariableRange' found in JSON Spec
      optionals?: { allowable_ranges?: VariableRange[]; allowable_values?: unknown[]; sc_name?: string },
  ): ENUM<N, E>;

  // @ts-ignore : 'Commands' found in generated code
  function A(...args: [TemplateStringsArray, ...string[]]): typeof Commands & typeof STEPS;
  // @ts-ignore : 'Commands' found in generated code
  function A(absoluteTime: Temporal.Instant): typeof Commands & typeof STEPS;
  // @ts-ignore : 'Commands' found in generated code
  function A(timeDOYString: string): typeof Commands & typeof STEPS;

  // @ts-ignore : 'Commands' found in generated code
  function R(...args: [TemplateStringsArray, ...string[]]): typeof Commands & typeof STEPS;
  // @ts-ignore : 'Commands' found in generated code
  function R(duration: Temporal.Duration): typeof Commands & typeof STEPS;
  // @ts-ignore : 'Commands' found in generated code
  function R(timeHMSString: string): typeof Commands & typeof STEPS;

  // @ts-ignore : 'Commands' found in generated code
  function E(...args: [TemplateStringsArray, ...string[]]): typeof Commands & typeof STEPS;
  // @ts-ignore : 'Commands' found in generated code
  function E(duration: Temporal.Duration): typeof Commands & typeof STEPS;
  // @ts-ignore : 'Commands' found in generated code
  function E(timeHMSString: string): typeof Commands & typeof STEPS;

  // @ts-ignore : 'Commands' found in generated code
  const C: typeof Commands & typeof STEPS;
}

/**
 *  ---------------------------------
 * 			 Sequence eDSL
 * ---------------------------------
 */

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

  public static new<
      // @ts-ignore : 'VariableDeclaration' found in JSON Spec
      const Locals extends ReadonlyArray<VariableDeclaration>,
      // @ts-ignore : 'VariableDeclaration' found in JSON Spec
      const Parameters extends ReadonlyArray<VariableDeclaration>,
  >(
      opts:
          | {
        seqId: string;
        // @ts-ignore : 'Metadata' found in JSON Spec
        metadata: Metadata;
        locals?: Locals;
        parameters?: Parameters;
        steps: // @ts-ignore : 'Step' found in JSON Spec
            | Step[]
            | ((opts: {
          /*Fancy way to map our array of objects to an object with name as key and type as value
           * this needs to be inlined instead of extracted to a helper type alias so that monaco won't hide the underlying type behind the alias
           */
          locals: { [Index in Locals[number] as Index['name']]: Index['type'] };
          parameters: { [Index in Parameters[number] as Index['name']]: Index['type'] };
          // @ts-ignore : 'Step' found in JSON Spec
        }) => Step[]);
        // @ts-ignore : 'ImmediateCommand' found in JSON Spec
        immediate_commands?: ImmediateCommand[];
        // @ts-ignore : 'HardwareCommand' found in JSON Spec
        hardware_commands?: HardwareCommand[];
        // @ts-ignore : 'Request' found in JSON Spec
        requests?: Request[] | ((opts : {
          locals: { [Index in Locals[number] as Index['name']]: Index['type'] };
          parameters: { [Index in Parameters[number] as Index['name']]: Index['type'] };
          // @ts-ignore : 'Step' found in JSON Spec
        }) => Request[]);
      }
          // @ts-ignore : 'SeqJson' found in JSON Spec
          | SeqJson,
  ): Sequence {
    if ('id' in opts) {
      // @ts-ignore : 'SeqJson' found in JSON Spec
      return new Sequence(opts as SeqJson);
    } else {
      /** Forcing the correct type for the undefined case */
      const seqId = 'id' in opts ? opts.id : opts.seqId;
      const metadata = opts.metadata;
      const immediate_commands = opts.immediate_commands;
      const hardware_commands = opts.hardware_commands;

      const locals = opts.locals ?? ([] as unknown as Locals);
      const parameters = opts.parameters ?? ([] as unknown as Parameters);

      // @ts-ignore
      const localsMap = locals.reduce((acc, declaration) => {
        /* ts ignore here because we're very much NOT doing what we're telling TS we are, but it makes for good UX
         * we're not actually passing the type property of the declaration, but the whole declaration
         * This ensures we get a readable type in the steps function
         */
        // @ts-ignore
        const variable = Variable.new({ name: declaration.name, type: declaration.type });
        variable.setKind('locals');
        acc[declaration.name] = variable;
        return acc;
      }, {} as { [Index in Locals[number] as Index['name']]: Index['type'] });

      // @ts-ignore
      const parametersMap = parameters.reduce((acc, declaration) => {
        // ts ignore here because we're very much NOT doing what we're telling TS we are, but it makes for good UX
        // @ts-ignore
        const variable = Variable.new({ name: declaration.name, type: declaration.type });
        variable.setKind('parameters');
        acc[declaration.name] = variable;
        return acc;
      }, {} as { [Index in Parameters[number] as Index['name']]: Index['type'] });

      const steps =
          typeof opts.steps === 'function' ? opts.steps({ locals: localsMap, parameters: parametersMap }) : opts.steps;

      const requests =
          typeof opts.requests === 'function' ? opts.requests({ locals: localsMap, parameters: parametersMap }) : opts.requests;

      return new Sequence({
        seqId,
        metadata,
        locals: locals.length !== 0 ? [locals[0], ...locals.slice(1)] : undefined,
        parameters: parameters.length !== 0 ? [parameters[0], ...parameters.slice(1)] : undefined,
        steps,
        immediate_commands,
        hardware_commands,
        requests
      } as SequenceOptions);
    }
  }

  // @ts-ignore : 'SeqJson' found in JSON Spec
  public toSeqJson(): SeqJson {
    return {
      id: this.id,
      metadata: this.metadata,
      ...(this.steps
        ? {
            steps: this.steps.map(step => {
              if (step instanceof CommandStem || step instanceof Ground_Block || step instanceof Ground_Event)
                return step.toSeqJson();
              return step;
            }),
          }
          : {}),
      ...(this.locals
          ? {
            locals: [
              Variable.isVariable(this.locals[0]) ? Variable.new(this.locals[0]).toSeqJson() : this.locals[0],
              ...this.locals.slice(1).map(local => {
                if (Variable.isVariable(local)) return Variable.new(local).toSeqJson();
                return local;
              }),
            ],
          }
          : {}),
      ...(this.parameters
          ? {
            parameters: [
              Variable.isVariable(this.parameters[0])
                  ? Variable.new(this.parameters[0]).toSeqJson()
                  : this.parameters[0],
              ...this.parameters.slice(1).map(parameter => {
                if (Variable.isVariable(parameter)) return Variable.new(parameter).toSeqJson();
                return parameter;
              }),
            ],
          }
          : {}),
      ...(this.requests
        ? {
            requests: this.requests.map(request => {
              return {
                name: request.name,
                steps: [
                  request.steps[0] instanceof CommandStem ||
                  request.steps[0] instanceof Ground_Block ||
                  request.steps[0] instanceof Ground_Event
                    ? request.steps[0].toSeqJson()
                    : request.steps[0],
                  // @ts-ignore : 'step' found in JSON Spec
                  ...request.steps.slice(1).map(step => {
                    if (step instanceof CommandStem || step instanceof Ground_Block || step instanceof Ground_Event)
                      return step.toSeqJson();
                    return step;
                  }),
                ],
                type: request.type,
                ...(request.description ? { description: request.description } : {}),
                ...(request.ground_epoch ? { ground_epoch: request.ground_epoch } : {}),
                ...(request.time ? { time: request.time } : {}),
                ...(request.metadata ? { metadata: request.metadata } : {}),
              };
            }),
          }
        : {}),
      ...(this.immediate_commands
        ? {
            immediate_commands: this.immediate_commands.map(command => {
              if (command instanceof ImmediateStem) return command.toSeqJson();
              if (command instanceof CommandStem)
                return {
                  args: [
                    {
                      name: 'message',
                      type: 'string',
                      value: `ERROR: ${command.toEDSLString()}, is not an immediate command.`,
                    },
                  ],
                  stem: '$$ERROR$$',
                };
              else return command;
            }),
          }
        : {}),
      ...(this.hardware_commands
        ? {
            hardware_commands: this.hardware_commands.map(h => {
              return h instanceof HardwareStem ? h.toSeqJson() : h;
            }),
          }
        : {}),
    };
  }

  public toEDSLString(): string {
    const commandsString =
      this.steps && this.steps.length > 0
        ? '[\n' +
          indent(
            this.steps
              .map(step => {
                if (step instanceof CommandStem || step instanceof Ground_Block || step instanceof Ground_Event) {
                  return step.toEDSLString() + ',';
                }
                return objectToString(step) + ',';
              })
              .join('\n'),
            1,
          ) +
          '\n]'
        : '';
    //ex.
    // [C.ADD_WATER]
    const metadataString = Object.keys(this.metadata).length == 0 ? `{}` : `${objectToString(this.metadata)}`;

    const localsString = this.locals
        ? '[\n' +
        indent(
            this.locals
                .map(local => {
                  if (local instanceof Variable) {
                    return local.toEDSLString();
                  } else if (Variable.isVariable(local)) {
                    return Variable.new(local).toEDSLString();
                  }
                  return objectToString(local);
                })
                .join('\n'),
            1,
        ) +
        '\n]'
        : '';
    //ex.
    // `locals: [
    //	ENUM('duration','WHEEL_DURATION'),
    //  ]`;

    const parameterString = this.parameters
        ? '[\n' +
        indent(
            this.parameters
                .map(parameter => {
                  if (parameter instanceof Variable) {
                    return parameter.toEDSLString();
                  } else if (Variable.isVariable(parameter)) {
                    return Variable.new(parameter).toEDSLString();
                  }
                  return objectToString(parameter);
                })
                .join('\n'),
            1,
        ) +
        '\n]'
        : '';
    //ex.
    // `parameters: [
    //	FLOAT('duration', { sc_name : 'test'}),
    //  ]`;

    const hardwareString = this.hardware_commands
      ? `[\n${indent(this.hardware_commands.map(h => (h as HardwareStem).toEDSLString()).join(',\n'), 1)}\n]`
      : '';
    //ex.
    // hardware_commands: [
    //   HWD_PYRO_BURN,
    // ],

    const immediateString =
      this.immediate_commands && this.immediate_commands.length > 0
        ? '[\n' +
          indent(
            this.immediate_commands
              .map(command => {
                if (command instanceof ImmediateStem) {
                  return command.toEDSLString() + ',';
                }
                return objectToString(command) + ',';
              })
              .join('\n'),
            1,
          ) +
          '\n]'
        : '';
    //ex.
    // immediate_commands: [ADD_WATER]

    const requestString = this.requests
      ? `[\n${indent(
          this.requests
            .map(r => {
              return (
                `{\n` +
                indent(
                  `name: '${r.name}',\n` +
                    `steps: [\n${indent(
                      r.steps
                        // @ts-ignore : 's: Step' found in JSON Spec
                        .map(s => {
                          if (s instanceof CommandStem || s instanceof Ground_Block || s instanceof Ground_Event) {
                            return s.toEDSLString() + ',';
                          }
                          return objectToString(s) + ',';
                        })
                        .join('\n'),
                      1,
                    )}\n],` +
                    `\ntype: '${r.type}',` +
                    `${r.description ? `\ndescription: '${r.description}',` : ''}` +
                    `${r.ground_epoch ? `\nground_epoch: ${objectToString(r.ground_epoch)},` : ''}` +
                    `${r.time ? `\ntime: ${objectToString(r.time)},` : ''}` +
                    `${r.metadata ? `\nmetadata: ${objectToString(r.metadata)},` : ''}`,
                  1,
                ) +
                `\n}`
              );
            })
            .join(',\n'),
          1,
        )}\n]`
      : '';
    //ex.
    /*requests: [
    {
      name: 'power',
      steps: [
      R`04:39:22.000`.PREHEAT_OVEN({
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
      author: 'rrgoet',
      },
    }
    ]
    }*/

    return (
        `export default () =>\n` +
        `${indent(`Sequence.new({`, 1)}\n` +
        `${indent(`seqId: '${this.id}'`, 2)},\n` +
        `${indent(`metadata: ${metadataString}`, 2)},\n` +
        `${localsString.length !== 0 ? `${indent(`locals: ${localsString}`, 2)},\n` : ''}` +
        `${parameterString.length !== 0 ? `${indent(`parameters: ${parameterString}`, 2)},\n` : ''}` +
        `${
            commandsString.length !== 0 ? `${indent(`steps: ({ locals, parameters }) => (${commandsString}`, 2)}),\n` : ''
        }` +
        `${hardwareString.length !== 0 ? `${indent(`hardware_commands: ${hardwareString}`, 2)},\n` : ''}` +
        `${immediateString.length !== 0 ? `${indent(`immediate_commands: ${immediateString}`, 2)},\n` : ''}` +
        `${requestString.length !== 0 ? `${indent(`requests: ({ locals, parameters }) => (${requestString}`, 2)}),\n` : ''}` +
        `${indent(`});`, 1)}`
    );
  }

  // @ts-ignore : 'Args' found in JSON Spec
  public static fromSeqJson(json: SeqJson): Sequence {
    // @ts-ignore : 'VariableDeclaration' found in JSON Spec
    const localNames = json.locals !== undefined ? json.locals.map( (local : VariableDeclaration) => local.name) : []
    // @ts-ignore : 'VariableDeclaration' found in JSON Spec
    const parameterNames = json.locals !== undefined ? json.parameters.map( (parameter : VariableDeclaration) => parameter.name) : []

    return Sequence.new({
      id: json.id,
      metadata: json.metadata,
      // @ts-ignore : 'Step' found in JSON Spec
      ...(json.steps
        ? {
            // @ts-ignore : 'Step' found in JSON Spec
            steps: json.steps.map((c: Step) => {
              if (c.type === 'command') return CommandStem.fromSeqJson(c as CommandStem, localNames, parameterNames);
              else if (c.type === 'ground_block') return Ground_Block.fromSeqJson(c as Ground_Block);
              else if (c.type === 'ground_event') return Ground_Event.fromSeqJson(c as Ground_Event);
              return c;
            }),
          }
          : {}),
      ...(json.locals
          ? {
            locals: [
              Variable.fromSeqJson(json.locals[0], 'locals'),
              // @ts-ignore : 'l: Request' found in JSON Spec
              ...json.locals.slice(1).map(l => {
                return Variable.fromSeqJson(l, 'locals');
              }),
            ],
          }
          : {}),
      ...(json.parameters
          ? {
            parameters: [
              Variable.fromSeqJson(json.parameters[0], 'parameters'),
              // @ts-ignore : 'l: Request' found in JSON Spec
              ...json.parameters.slice(1).map(p => {
                return Variable.fromSeqJson(p, 'parameters');
              }),
            ],
          }
          : {}),
      ...(json.requests
        ? {
            // @ts-ignore : 'r: Request' found in JSON Spec
            requests: json.requests.map(r => {
              return {
                name: r.name,
                type: r.type,
                ...(r.description ? { description: r.description } : {}),
                ...(r.ground_epoch ? { ground_epoch: r.ground_epoch } : {}),
                ...(r.time ? { time: r.time } : {}),
                ...(r.metadata ? { metadata: r.metadata } : {}),
                steps: [
                  r.steps[0].type === 'command'
                    ? CommandStem.fromSeqJson(r.steps[0] as CommandStem,localNames, parameterNames)
                    : r.steps[0].type === 'ground_block'
                    ? // @ts-ignore : 'GroundBlock' found in JSON Spec
                      Ground_Block.fromSeqJson(r.steps[0] as GroundBlock)
                    : r.steps[0].type === 'ground_event'
                    ? // @ts-ignore : 'GroundEvent' found in JSON Spec
                      Ground_Event.fromSeqJson(r.steps[0] as GroundEvent)
                    : r.steps[0],
                  // @ts-ignore : 'step : Step' found in JSON Spec
                  ...r.steps.slice(1).map(step => {
                    if (step.type === 'command') return CommandStem.fromSeqJson(step as CommandStem, localNames, parameterNames);
                    else if (step.type === 'ground_block') return Ground_Block.fromSeqJson(step as Ground_Block);
                    else if (step.type === 'ground_event') return Ground_Event.fromSeqJson(step as Ground_Event);
                    return step;
                  }),
                ],
              };
            }),
          }
        : {}),
      ...(json.immediate_commands
        ? {
            // @ts-ignore : 'Step' found in JSON Spec
            immediate_commands: json.immediate_commands.map((c: ImmediateCommand) => ImmediateStem.fromSeqJson(c)),
          }
        : {}),
      ...(json.hardware_commands
        ? // @ts-ignore : 'HardwareCommand' found in JSON Spec
          { hardware_commands: json.hardware_commands.map((h: HardwareCommand) => HardwareStem.fromSeqJson(h)) }
          : {}),
      // @ts-ignore : 'SeqJson' found in JSON Spec
    } as SeqJson);
  }
}

/**
 * ----------------------------------------
 * 			Local and Parameters
 * ----------------------------------------
 */

//@ts-ignore : 'VariableDeclaration: Request' found in JSON Spec
export class Variable implements VariableDeclaration {
  name: string;
  type: VariableType;

  [k: string]: unknown;
  private kind: 'locals' | 'parameters' | 'unknown' = 'locals';
  private readonly _enum_name?: string | undefined;
  // @ts-ignore : 'VariableRange: Request' found in JSON Spec
  private readonly _allowable_ranges?: VariableRange[] | undefined;
  private readonly _allowable_values?: unknown[] | undefined;
  private readonly _sc_name?: string | undefined;

  constructor(opts: VariableOptions) {
    this.name = opts.name;
    this.type = opts.type;

    this._enum_name = opts.enum_name ?? undefined;
    this._allowable_ranges = opts.allowable_ranges ?? undefined;
    this._allowable_values = opts.allowable_values ?? undefined;
    this._sc_name = opts.sc_name ?? undefined;
  }

  public static new(opts: VariableOptions): Variable {
    return new Variable(opts);
  }

  public static isVariable(obj: any): obj is Variable {
    return obj && typeof obj === 'object' && obj.hasOwnProperty('name') && obj.hasOwnProperty('type');
  }

  public setKind(kind: 'locals' | 'parameters' | 'unknown') {
    this.kind = kind;
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public toSeqJson(): VariableDeclaration {
    let error;
    //check if type ENUM has ENUM_NAME set
    if (this.type === 'ENUM' && !this._enum_name) {
      error = `$$ERROR$$: 'enum_name' is required for ENUM type.`;
    }
    // check if type ins't ENUM but has a ENUM_NAME set
    if (this.type !== 'ENUM' && this._enum_name) {
      error = `$$ERROR$$: 'enum_name: ${this._enum_name}' is not required for non-ENUM type.`;
    }
    // check if type is STRING but has allowable_ranges set
    if (this.type === 'STRING' && this._allowable_ranges) {
      error = `$$ERROR$$: 'allowable_ranges' is not required for STRING type.`;
    }

    return {
      name: error ? error : this.name,
      type: this.type,
      ...(this._enum_name && { enum_name: this._enum_name }),
      ...(this._allowable_ranges && {
        allowable_ranges: this._allowable_ranges.map(range => {
          return {
            min: range.min,
            max: range.max,
          };
        }),
      }),
      ...(this._allowable_values && { allowable_values: this._allowable_values }),
      ...(this._sc_name && { sc_name: this._sc_name })
    };
  }

  // @ts-ignore : 'VariableDeclaration: Request' found in JSON Spec
  public static fromSeqJson(json: VariableDeclaration, kind: 'locals' | 'parameters' | 'unknown'): Variable {
    const variable = new Variable({
      name: json.name,
      type: json.type as VariableType,
      ...(json.enum_name ? { enum_name: json.enum_name } : {}),
      ...(json.allowable_ranges ? { allowable_ranges: json.allowable_ranges } : {}),
      ...(json.allowable_values ? { allowable_values: json.allowable_values } : {}),
      ...(json.sc_name ? { sc_name: json.sc_name } : {}),
    });
    variable.setKind(kind);
    return variable;
  }

  public toReferenceString(): string {
    return `${this.kind}.${this.name}`;
  }

  public toEDSLString(): string {
    const types = ['FLOAT', 'UINT', 'INT', 'STRING', 'ENUM'];
    const type = types.includes(this.type) ? this.type : 'UNKNOWN';
    switch (type) {
      case 'FLOAT':
      case 'UINT':
      case 'INT':
        return `${type}('${this.name}'${
            this._allowable_ranges || this._allowable_values || this._sc_name
                ? ', ' +
                objectToString({
                  ...(this._allowable_ranges ? { allowable_ranges: this._allowable_ranges } : {}),
                  ...(this._allowable_values ? { allowable_values: this._allowable_values } : {}),
                  ...(this._sc_name ? { sc_name: this._sc_name } : {}),
                }) +
                ')'
                : ')'
        }`;
      case 'STRING':
        return `${type}('${this.name}'${
            this._allowable_ranges || this._allowable_values || this._sc_name
                ? ', ' +
                objectToString({
                  ...(this._allowable_values ? { allowable_values: this._allowable_values } : {}),
                  ...(this._sc_name ? { sc_name: this._sc_name } : {}),
                }) +
                ')'
                : ')'
        }`;
      case 'ENUM':
        return `${type}('${this.name}', '${this._enum_name}'${
            this._allowable_ranges || this._allowable_values || this._sc_name
                ? ', ' +
                objectToString({
                  ...(this._allowable_ranges ? { allowable_ranges: this._allowable_ranges } : {}),
                  ...(this._allowable_values ? { allowable_values: this._allowable_values } : {}),
                  ...(this._sc_name ? { sc_name: this._sc_name } : {}),
                }) +
                ')'
                : ')'
        }`;
      default:
        return `${type}(${this.name}${this._enum_name ? ', ' + this._enum_name : ''}${
            this._allowable_ranges || this._allowable_values || this._sc_name
                ? ', ' +
                objectToString({
                  ...(this._allowable_ranges ? { allowable_ranges: this._allowable_ranges } : {}),
                  ...(this._allowable_values ? { allowable_values: this._allowable_values } : {}),
                  ...(this._sc_name ? { sc_name: this._sc_name } : {}),
                })
                : ''
        })`;
    }
  }
}

export function INT<N extends string>(name: N): INT<N>;
export function INT<N extends string>(
    name: N,
    optionals: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): INT<N>;
export function INT<N extends string>(
    name: N,
    optionals?: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): INT<N> {
  const { allowable_ranges, allowable_values, sc_name } = optionals || {};
  return { name, type: VariableType.INT, allowable_ranges, allowable_values, sc_name };
}

export function UINT<N extends string>(name: N): UINT<N>;
export function UINT<N extends string>(
    name: N,
    optionals: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): UINT<N>;
export function UINT<N extends string>(
    name: N,
    optionals?: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): UINT<N> {
  const { allowable_ranges, allowable_values, sc_name } = optionals || {};
  return { name, type: VariableType.UINT, allowable_ranges, allowable_values, sc_name };
}

export function FLOAT<N extends string>(name: N): FLOAT<N>;
export function FLOAT<N extends string>(
    name: N,
    optionals: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): FLOAT<N>;
export function FLOAT<N extends string>(
    name: N,
    optionals?: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): FLOAT<N> {
  const { allowable_ranges, allowable_values, sc_name } = optionals || {};
  return { name, type: VariableType.FLOAT, allowable_ranges, allowable_values, sc_name };
}

export function STRING<N extends string>(name: N): STRING<N>;
export function STRING<N extends string>(
    name: N,
    optionals: {
      allowable_values?: unknown[];
      sc_name?: string;
    },
): STRING<N>;
export function STRING<N extends string>(
    name: N,
    optionals?: {
      allowable_values?: unknown[];
      sc_name?: string;
    },
): STRING<N> {
  const { allowable_values, sc_name } = optionals || {};
  return { name, type: VariableType.STRING, allowable_values, sc_name };
}

export function ENUM<const N extends string, const E extends string>(name: N, enum_name: E): ENUM<N, E>;
export function ENUM<const N extends string, const E extends string>(
    name: N,
    enum_name: E,
    optionals?: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): ENUM<N, E>;
export function ENUM<const N extends string, const E extends string>(
    name: N,
    enum_name: E,
    optionals?: {
      // @ts-ignore : 'VariableRange' found in JSON Spec
      allowable_ranges?: VariableRange[];
      allowable_values?: unknown[];
      sc_name?: string;
    },
): ENUM<N, E> {
  const { allowable_ranges, allowable_values, sc_name } = optionals || {};
  return { name, enum_name, type: VariableType.ENUM, allowable_ranges, allowable_values, sc_name };
}

/**
 * ---------------------------------
 *        STEPS eDSL
 * ---------------------------------
 */

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
      args: convertArgsToInterfaces(this.arguments),
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

  public static fromSeqJson(
      // @ts-ignore : 'Command' found in JSON Spec
      json: Command,
      localNames?: string[],
      parameterNames?: string[],
  ): CommandStem {
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
      arguments: convertInterfacesToArgs(json.args, localNames, parameterNames),
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
      metadata: this._metadata,
    });
  }

  public epochTiming(epochTime: Temporal.Duration): CommandStem<A> {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      epochTime: epochTime,
      metadata: this._metadata,
    });
  }

  public relativeTiming(relativeTime: Temporal.Duration): CommandStem<A> {
    return CommandStem.new({
      stem: this.stem,
      arguments: this.arguments,
      relativeTime: relativeTime,
      metadata: this._metadata,
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

    const argsString = Object.keys(this.arguments).length === 0 ? '' : `(${argumentsToString(this.arguments)})`;

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
}

// @ts-ignore : 'Args' found in JSON Spec
export class ImmediateStem<A extends Args[] | { [argName: string]: any } = [] | {}> implements ImmediateCommand {
  public readonly arguments: A;
  public readonly stem: string;
  // @ts-ignore : 'Args' found in JSON Spec
  public readonly args!: Args;
  // @ts-ignore : 'Metadata' found in JSON Spec
  private readonly _metadata?: Metadata | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  private readonly _description?: Description | undefined;

  private constructor(opts: ImmediateOptions<A>) {
    this.stem = opts.stem;
    this.arguments = opts.arguments;
    this._metadata = opts.metadata;
    this._description = opts.description;
  }

  public static new<A extends any[] | { [argName: string]: any }>(opts: ImmediateOptions<A>): ImmediateStem<A> {
    return new ImmediateStem<A>(opts);
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public METADATA(metadata: Metadata): ImmediateStem {
    return ImmediateStem.new({
      stem: this.stem,
      arguments: this.arguments,
      metadata: metadata,
      description: this._description,
    });
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public GET_METADATA(): Metadata | undefined {
    return this._metadata;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public DESCRIPTION(description: Description): ImmediateStem {
    return ImmediateStem.new({
      stem: this.stem,
      arguments: this.arguments,
      metadata: this._metadata,
      description: description,
    });
  }
  // @ts-ignore : 'Description' found in JSON Spec
  public GET_DESCRIPTION(): Description | undefined {
    return this._description;
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public toSeqJson(): ImmediateCommand {
    return {
      args: convertArgsToInterfaces(this.arguments),
      stem: this.stem,
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._description ? { description: this._description } : {}),
    };
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public static fromSeqJson(json: ImmediateCommand, localNames?: string[], parameterNames?: string[]): ImmediateStem {
    return ImmediateStem.new({
      stem: json.stem,
      arguments: convertInterfacesToArgs(json.args, localNames, parameterNames),
      metadata: json.metadata,
      description: json.description,
    });
  }

  public toEDSLString(): string {
    const argsString = Object.keys(this.arguments).length === 0 ? '' : `(${argumentsToString(this.arguments)})`;

    const metadata =
      this._metadata && Object.keys(this._metadata).length !== 0
        ? `\n.METADATA(${objectToString(this._metadata)})`
        : '';
    const description =
      this._description && this._description.length !== 0 ? `\n.DESCRIPTION('${this._description}')` : '';

    return `${this.stem}${argsString}${description}${metadata}`;
  }
}

//The function takes an object of arguments and converts them into the Args type. It does this by looping through the
// values and pushing a new argument type to the result array depending on the type of the value.
// If the value is an array, it will create a RepeatArgument type and recursively call on the values of the array.
// the function returns the result array of argument types -
// StringArgument, NumberArgument, BooleanArgument, SymbolArgument, HexArgument, and RepeatArgument.
// @ts-ignore : 'Args' found in JSON Spec
function convertArgsToInterfaces(args: { [argName: string]: any }): Args {
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
        value: value.map(arg => {
          return convertRepeatArgs(arg);
        }),
        type: 'repeat',
        name: key,
      };
      result.push(repeatArg);
    } else {
      result = result.concat(convertValueToObject(value, key));
    }
  }
  return result;
}

// @ts-ignore : 'GroundBlock' found in JSON Spec
export class Ground_Block implements GroundBlock {
  name: string;
  // @ts-ignore : 'Time' found in JSON Spec
  time!: Time;
  type: 'ground_block' = 'ground_block';

  private readonly _absoluteTime: Temporal.Instant | null = null;
  private readonly _epochTime: Temporal.Duration | null = null;
  private readonly _relativeTime: Temporal.Duration | null = null;

  // @ts-ignore : 'Args' found in JSON Spec
  private readonly _args: Args | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  private readonly _description: Description | undefined;
  // @ts-ignore : 'Metadata' found in JSON Spec
  private readonly _metadata: Metadata | undefined;
  // @ts-ignore : 'Model' found in JSON Spec
  private readonly _models: Model[] | undefined;

  constructor(opts: GroundOptions) {
    this.name = opts.name;

    this._args = opts.args ?? undefined;
    this._description = opts.description ?? undefined;
    this._metadata = opts.metadata ?? undefined;
    this._models = opts.models ?? undefined;

    if ('absoluteTime' in opts) {
      this._absoluteTime = opts.absoluteTime;
    } else if ('epochTime' in opts) {
      this._epochTime = opts.epochTime;
    } else if ('relativeTime' in opts) {
      this._relativeTime = opts.relativeTime;
    }
  }

  public static new(opts: GroundOptions): Ground_Block {
    return new Ground_Block(opts);
  }

  public absoluteTiming(absoluteTime: Temporal.Instant): Ground_Block {
    return new Ground_Block({
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { model: this._models } : {}),
      name: this.name,
      absoluteTime: absoluteTime,
    });
  }

  public epochTiming(epochTime: Temporal.Duration): Ground_Block {
    return new Ground_Block({
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { model: this._models } : {}),
      name: this.name,
      epochTime: epochTime,
    });
  }

  public relativeTiming(relativeTime: Temporal.Duration): Ground_Block {
    return new Ground_Block({
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { model: this._models } : {}),
      name: this.name,
      relativeTime: relativeTime,
    });
  }

  // @ts-ignore : 'Model' found in JSON Spec
  public MODELS(models: Model[]): Ground_Block {
    return Ground_Block.new({
      name: this.name,
      models: models,
      ...(this._args && { args: this._args }),
      ...(this._description && { description: this._description }),
      ...(this._metadata && { metadata: this._metadata }),
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }

  // @ts-ignore : 'Model' found in JSON Spec
  public GET_MODELS(): Model[] | undefined {
    return this._models;
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public METADATA(metadata: Metadata): Ground_Block {
    return Ground_Block.new({
      name: this.name,
      ...(this._models && { models: this._models }),
      ...(this._args && { args: this._args }),
      ...(this._description && { description: this._description }),
      metadata: metadata,
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public GET_METADATA(): Metadata | undefined {
    return this._metadata;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public DESCRIPTION(description: Description): Ground_Block {
    return Ground_Block.new({
      name: this.name,
      ...(this._models && { models: this._models }),
      ...(this._args && { args: this._args }),
      description: description,
      ...(this._metadata && { metadata: this._metadata }),
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }
  // @ts-ignore : 'Description' found in JSON Spec
  public GET_DESCRIPTION(): Description | undefined {
    return this._description;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public ARGUMENTS(args: Args): Ground_Block {
    return Ground_Block.new({
      name: this.name,
      ...(this._models && { models: this._models }),
      args: args,
      ...(this._description && { description: this._description }),
      ...(this._metadata && { metadata: this._metadata }),
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public GET_ARGUMENTS(): Args | undefined {
    return this._args;
  }

  // @ts-ignore : 'GroundBlock' found in JSON Spec
  public toSeqJson(): GroundBlock {
    return {
      name: this.name,
      time:
        this._absoluteTime !== null
          ? { type: TimingTypes.ABSOLUTE, tag: instantToDoy(this._absoluteTime) }
          : this._epochTime !== null
          ? { type: TimingTypes.EPOCH_RELATIVE, tag: durationToHms(this._epochTime) }
          : this._relativeTime !== null
          ? { type: TimingTypes.COMMAND_RELATIVE, tag: durationToHms(this._relativeTime) }
          : { type: TimingTypes.COMMAND_COMPLETE },
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { models: this._models } : {}),
      type: this.type,
    };
  }

  // @ts-ignore : 'GroundBlock' found in JSON Spec
  public static fromSeqJson(json: GroundBlock): Ground_Block {
    const timeValue =
      json.time.type === TimingTypes.ABSOLUTE
        ? { absoluteTime: doyToInstant(json.time.tag as DOY_STRING) }
        : json.time.type === TimingTypes.COMMAND_RELATIVE
        ? { relativeTime: hmsToDuration(json.time.tag as HMS_STRING) }
        : json.time.type === TimingTypes.EPOCH_RELATIVE
        ? { epochTime: hmsToDuration(json.time.tag as HMS_STRING) }
        : {};

    return Ground_Block.new({
      name: json.name,
      ...(json.args ? { args: json.args } : {}),
      ...(json.description ? { description: json.description } : {}),
      ...(json.metadata ? { metadata: json.metadata } : {}),
      ...(json.models ? { models: json.models } : {}),
      ...timeValue,
    });
  }

  public toEDSLString(): string {
    const timeString = this._absoluteTime
      ? `A\`${instantToDoy(this._absoluteTime)}\``
      : this._epochTime
      ? `E\`${durationToHms(this._epochTime)}\``
      : this._relativeTime
      ? `R\`${durationToHms(this._relativeTime)}\``
      : 'C';

    const args =
      this._args && Object.keys(this._args).length !== 0
        ? // @ts-ignore : 'A : Args' found in JSON Spec
          `\n.ARGUMENTS([\n${this._args.map(a => indent(objectToString(a))).join(',\n')}\n])`
        : '';

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

    return `${timeString}.GROUND_BLOCK('${this.name}')${args}${description}${metadata}${models}`;
  }
}

/**
 * This is a Ground Block step
 *
 */
function GROUND_BLOCK(name: string) {
  return new Ground_Block({ name: name });
}

// @ts-ignore : 'GroundBlock' found in JSON Spec
export class Ground_Event implements GroundEvent {
  name: string;
  // @ts-ignore : 'Time' found in JSON Spec
  time!: Time;
  type: 'ground_event' = 'ground_event';

  private readonly _absoluteTime: Temporal.Instant | null = null;
  private readonly _epochTime: Temporal.Duration | null = null;
  private readonly _relativeTime: Temporal.Duration | null = null;

  // @ts-ignore : 'Args' found in JSON Spec
  private readonly _args: Args | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  private readonly _description: Description | undefined;
  // @ts-ignore : 'Metadata' found in JSON Spec
  private readonly _metadata: Metadata | undefined;
  // @ts-ignore : 'Model' found in JSON Spec
  private readonly _models: Model[] | undefined;

  constructor(opts: GroundOptions) {
    this.name = opts.name;

    this._args = opts.args ?? undefined;
    this._description = opts.description ?? undefined;
    this._metadata = opts.metadata ?? undefined;
    this._models = opts.models ?? undefined;

    if ('absoluteTime' in opts) {
      this._absoluteTime = opts.absoluteTime;
    } else if ('epochTime' in opts) {
      this._epochTime = opts.epochTime;
    } else if ('relativeTime' in opts) {
      this._relativeTime = opts.relativeTime;
    }
  }

  public static new(opts: GroundOptions): Ground_Event {
    return new Ground_Event(opts);
  }

  public absoluteTiming(absoluteTime: Temporal.Instant): Ground_Event {
    return new Ground_Event({
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { model: this._models } : {}),
      name: this.name,
      absoluteTime: absoluteTime,
    });
  }

  public epochTiming(epochTime: Temporal.Duration): Ground_Event {
    return new Ground_Event({
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { model: this._models } : {}),
      name: this.name,
      epochTime: epochTime,
    });
  }

  public relativeTiming(relativeTime: Temporal.Duration): Ground_Event {
    return new Ground_Event({
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { model: this._models } : {}),
      name: this.name,
      relativeTime: relativeTime,
    });
  }

  // @ts-ignore : 'Model' found in JSON Spec
  public MODELS(models: Model[]): Ground_Event {
    return Ground_Event.new({
      name: this.name,
      models: models,
      ...(this._args && { args: this._args }),
      ...(this._description && { description: this._description }),
      ...(this._metadata && { metadata: this._metadata }),
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }

  // @ts-ignore : 'Model' found in JSON Spec
  public GET_MODELS(): Model[] | undefined {
    return this._models;
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public METADATA(metadata: Metadata): Ground_Event {
    return Ground_Event.new({
      name: this.name,
      ...(this._models && { models: this._models }),
      ...(this._args && { args: this._args }),
      ...(this._description && { description: this._description }),
      metadata: metadata,
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public GET_METADATA(): Metadata | undefined {
    return this._metadata;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public DESCRIPTION(description: Description): Ground_Event {
    return Ground_Event.new({
      name: this.name,
      ...(this._models && { models: this._models }),
      ...(this._args && { args: this._args }),
      description: description,
      ...(this._metadata && { metadata: this._metadata }),
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }
  // @ts-ignore : 'Description' found in JSON Spec
  public GET_DESCRIPTION(): Description | undefined {
    return this._description;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public ARGUMENTS(args: Args): Ground_Event {
    return Ground_Event.new({
      name: this.name,
      ...(this._models && { models: this._models }),
      args: args,
      ...(this._description && { description: this._description }),
      ...(this._metadata && { metadata: this._metadata }),
      ...(this._absoluteTime && { absoluteTime: this._absoluteTime }),
      ...(this._epochTime && { epochTime: this._epochTime }),
      ...(this._relativeTime && { relativeTime: this._relativeTime }),
    });
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public GET_ARGUMENTS(): Args | undefined {
    return this._args;
  }

  // @ts-ignore : 'Ground_Event' found in JSON Spec
  public toSeqJson(): GroundEvent {
    return {
      name: this.name,
      time:
        this._absoluteTime !== null
          ? { type: TimingTypes.ABSOLUTE, tag: instantToDoy(this._absoluteTime) }
          : this._epochTime !== null
          ? { type: TimingTypes.EPOCH_RELATIVE, tag: durationToHms(this._epochTime) }
          : this._relativeTime !== null
          ? { type: TimingTypes.COMMAND_RELATIVE, tag: durationToHms(this._relativeTime) }
          : { type: TimingTypes.COMMAND_COMPLETE },
      ...(this._args ? { args: this._args } : {}),
      ...(this._description ? { description: this._description } : {}),
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._models ? { models: this._models } : {}),
      type: this.type,
    };
  }

  // @ts-ignore : 'GroundEvent' found in JSON Spec
  public static fromSeqJson(json: GroundEvent): Ground_Event {
    const timeValue =
      json.time.type === TimingTypes.ABSOLUTE
        ? { absoluteTime: doyToInstant(json.time.tag as DOY_STRING) }
        : json.time.type === TimingTypes.COMMAND_RELATIVE
        ? { relativeTime: hmsToDuration(json.time.tag as HMS_STRING) }
        : json.time.type === TimingTypes.EPOCH_RELATIVE
        ? { epochTime: hmsToDuration(json.time.tag as HMS_STRING) }
        : {};

    return Ground_Event.new({
      name: json.name,
      ...(json.args ? { args: json.args } : {}),
      ...(json.description ? { description: json.description } : {}),
      ...(json.metadata ? { metadata: json.metadata } : {}),
      ...(json.models ? { models: json.models } : {}),
      ...timeValue,
    });
  }

  public toEDSLString(): string {
    const timeString = this._absoluteTime
      ? `A\`${instantToDoy(this._absoluteTime)}\``
      : this._epochTime
      ? `E\`${durationToHms(this._epochTime)}\``
      : this._relativeTime
      ? `R\`${durationToHms(this._relativeTime)}\``
      : 'C';

    const args =
      this._args && Object.keys(this._args).length !== 0
        ? // @ts-ignore : 'A : Args' found in JSON Spec
          `\n.ARGUMENTS([\n${this._args.map(a => indent(objectToString(a))).join(',\n')}\n])`
        : '';

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

    return `${timeString}.GROUND_EVENT('${this.name}')${args}${description}${metadata}${models}`;
  }
}

/**
 * This is a Ground Event step
 *
 */
function GROUND_EVENT(name: string) {
  return new Ground_Event({ name: name });
}

export const STEPS = {
  GROUND_BLOCK: GROUND_BLOCK,
  GROUND_EVENT: GROUND_EVENT,
};

/**
 * -----------------------------------
 *        HW Commands
 * -----------------------------------
 */
// @ts-ignore : 'HardwareCommand' found in JSON Spec
export class HardwareStem implements HardwareCommand {
  public readonly stem: string;
  // @ts-ignore : 'Metadata' found in JSON Spec
  private readonly _metadata?: Metadata | undefined;
  // @ts-ignore : 'Description' found in JSON Spec
  private readonly _description?: Description | undefined;

  private constructor(opts: HardwareOptions) {
    this.stem = opts.stem;
    this._metadata = opts.metadata;
    this._description = opts.description;
  }

  public static new(opts: HardwareOptions): HardwareStem {
    return new HardwareStem(opts);
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public METADATA(metadata: Metadata): HardwareStem {
    return HardwareStem.new({
      stem: this.stem,
      metadata: metadata,
      description: this._description,
    });
  }

  // @ts-ignore : 'Metadata' found in JSON Spec
  public GET_METADATA(): Metadata | undefined {
    return this._metadata;
  }

  // @ts-ignore : 'Description' found in JSON Spec
  public DESCRIPTION(description: Description): HardwareStem {
    return HardwareStem.new({
      stem: this.stem,
      metadata: this._metadata,
      description: description,
    });
  }
  // @ts-ignore : 'Description' found in JSON Spec
  public GET_DESCRIPTION(): Description | undefined {
    return this._description;
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public toSeqJson(): HardwareCommand {
    return {
      stem: this.stem,
      ...(this._metadata ? { metadata: this._metadata } : {}),
      ...(this._description ? { description: this._description } : {}),
    };
  }

  // @ts-ignore : 'Command' found in JSON Spec
  public static fromSeqJson(json: HardwareCommand): HardwareStem {
    return HardwareStem.new({
      stem: json.stem,
      metadata: json.metadata,
      description: json.description,
    });
  }

  public toEDSLString(): string {
    const metadata =
      this._metadata && Object.keys(this._metadata).length !== 0
        ? `\n.METADATA(${objectToString(this._metadata)})`
        : '';
    const description =
      this._description && this._description.length !== 0 ? `\n.DESCRIPTION('${this._description}')` : '';

    return `${this.stem}${description}${metadata}`;
  }
}

/**
 *---------------------------------
 *      Time Utilities
 *---------------------------------
 */

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
function A(
  ...args: [TemplateStringsArray, ...string[]] | [Temporal.Instant] | [string]
): // @ts-ignore : Commands Used in generated code
typeof Commands & typeof STEPS {
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
function R(
  ...args: [TemplateStringsArray, ...string[]] | [Temporal.Duration] | [string]
): // @ts-ignore : Commands Used in generated code
typeof Commands & typeof STEPS {
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
function E(
  ...args: [TemplateStringsArray, ...string[]] | [Temporal.Duration] | [string]
): // @ts-ignore : Commands Used in generated code
typeof Commands & typeof STEPS {
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
  // @ts-ignore : Commands Used in generated code
): typeof Commands & typeof STEPS {
  return {
    // @ts-ignore : Commands Used in generated code
    ...Object.keys(Commands).reduce((accum, key) => {
      // @ts-ignore : Used in generated code
      const command = Commands[key as keyof Commands];

      if (typeof command === 'function') {
        //if (timeType === TimingTypes.ABSOLUTE) {
        accum[key] = (...args: Parameters<typeof command>): typeof command => {
          switch (timeType) {
            case TimingTypes.ABSOLUTE:
              return command(...args).absoluteTiming(timeValue);
            case TimingTypes.COMMAND_RELATIVE:
              return command(...args).relativeTiming(timeValue);
            case TimingTypes.EPOCH_RELATIVE:
              return command(...args).epochTiming(timeValue);
          }
        };
      } else {
        switch (timeType) {
          case TimingTypes.ABSOLUTE:
            accum[key] = command.absoluteTiming(timeValue);
            break;
          case TimingTypes.COMMAND_RELATIVE:
            accum[key] = command.relativeTiming(timeValue);
            break;
          case TimingTypes.EPOCH_RELATIVE:
            accum[key] = command.epochTiming(timeValue);
            break;
        }
      }

      return accum;
      // @ts-ignore : Used in generated code
    }, {} as typeof Commands),
    ...Object.keys(STEPS).reduce((accum, key) => {
      // @ts-ignore : Used in generated code
      const step = STEPS[key as keyof STEPS];
      // @ts-ignore : Used in generated code
      accum[key] = (...args: Parameters<typeof step>): typeof step => {
        switch (timeType) {
          case TimingTypes.ABSOLUTE:
            return step(...args).absoluteTiming(timeValue);
          case TimingTypes.COMMAND_RELATIVE:
            return step(...args).relativeTiming(timeValue);
          case TimingTypes.EPOCH_RELATIVE:
            return step(...args).epochTiming(timeValue);
        }
      };

      return accum;
    }, {} as typeof STEPS),
  };
}

/**
 * ---------------------------------
 *      Utility Functions
 * ---------------------------------
 */

// @ts-ignore : Used in generated code
function sortCommandArguments(args: { [argName: string]: any }, order: string[]): { [argName: string]: any } {
  if (typeof args[0] === 'object') {
    return Object.keys(args[0])
      .sort((a, b) => order.indexOf(a) - order.indexOf(b))
      .reduce((objectEntries: { [argName: string]: any }, key) => {
        if (Array.isArray(args[0][key])) {
          const sortedRepeatArgs = [];

          for (const test of args[0][key]) {
            sortedRepeatArgs.push(sortCommandArguments([test], order));
          }

          objectEntries[key] = sortedRepeatArgs;
        } else {
          objectEntries[key] = args[0][key];
        }

        return objectEntries;
      }, {});
  }

  return args;
}

function indent(text: string, numTimes: number = 1, char: string = '  '): string {
  return text
    .split('\n')
    .map(line => char.repeat(numTimes) + line)
    .join('\n');
}

// @ts-ignore : 'Args' found in JSON Spec
function argumentsToString<A extends Args[] | { [argName: string]: any } = [] | {}>(args: A): string {
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

/**
 * This function takes an array of Args interfaces and converts it into an object.
 * The interfaces array contains objects matching the ARGS interface.
 * Depending on the type property of each object, a corresponding object with the
 * name and value properties is created and added to the output.
 * Additionally, the function includes a validation function that prevents remote
 * property injection attacks.
 * @param interfaces
 */
// @ts-ignore : `Args` found in JSON Spec
function convertInterfacesToArgs(interfaces: Args, localNames?: String[], parameterNames?: String[]): {} | [] {
  const args = interfaces.length === 0 ? [] : {};

  // Use to prevent a Remote property injection attack
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
            return {
              [arg.name]: arg.value.map(
                  (
                      // @ts-ignore : found in JSON Spec
                      repeatArgBundle: (StringArgument | NumberArgument | BooleanArgument | SymbolArgument | HexArgument)[],
                  ) =>
                      repeatArgBundle.reduce((obj, item) => {
                        if (validate(item.name)) {
                          obj[item.name] = item.value;
                        }
                        return obj;
                      }, {}),
              ),
            };
          }
          return { repeat_error: 'Remote property injection detected...' };
        } else if (arg.type === 'symbol') {
          if (validate(arg.name)) {
            /**
             * We don't have access to the actual type of the variable, as it's not included in
             * the sequential JSON. However, we don't need the type at this point in the code. Instead,
             * we create a variable object with a default type of "INT". Later on in the code, the
             * variable will be used to generate "local.<name>" or "parameter.<name>" syntax in the toEDSLString() method.
             */
            let variable = Variable.new({ name: arg.value, type: VariableType.INT });
            if (localNames && localNames.includes(arg.value)) {
              variable.setKind('locals');
            } else if (parameterNames && parameterNames.includes(arg.value)) {
              variable.setKind('parameters');
            } else {
              const errorMsg = `Variable '${arg.value}' is not defined as a local or parameter`;
              variable = Variable.new({ name: `${arg.value} //ERROR: ${errorMsg}`, type: VariableType.INT });
              variable.setKind('unknown');
            }
            return { [arg.name]: variable };
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

  for (const key in convertedArgs) {
    Object.assign(args, convertedArgs[key]);
  }

  return args;
}

/**
 * The specific function to handle repeat args, we need to do this separately because
 * you cannot have a RepeatArgument inside a RepeatArgument.
 *
 * @param args
 * @returns
 */
function convertRepeatArgs(args: { [argName: string]: any }): any[] {
  let result: any[] = [];

  if (args['length'] === 0) {
    return result;
  }

  const values = Array.isArray(args) ? args[0] : args;

  for (let key in values) {
    result.push(convertValueToObject(values[key], key));
  }

  return result;
}

/**
 * This function takes a value and key and converts it to the correct object type supported by the seqjson spec.
 * The only type not supported here is RepeatArgument, as that is handled differently because you cannot have a
 * RepeatArgument inside a RepeatArgument.
 *
 * @param value
 * @param key
 * @returns An object for each type
 */
function convertValueToObject(value: any, key: string): any {
  switch (typeof value) {
    case 'string':
      return { type: 'string', value: value, name: key };
    case 'number':
      return { type: 'number', value: value, name: key };
    case 'boolean':
      return { type: 'boolean', value: value, name: key };
    default:
      if (
          value instanceof Object &&
          'name' in value &&
          'type' in value &&
          (value.type === 'FLOAT' ||
              value.type === 'INT' ||
              value.type === 'STRING' ||
              value.type === 'UINT' ||
              value.type === 'ENUM')
      ) {
        return { type: 'symbol', value: value.name, name: key };
      } else if (
          value instanceof Object &&
          value.hex &&
          value.hex === 'string' &&
          new RegExp('^0x([0-9A-F])+$').test(value.hex)
      ) {
        return { type: 'hex', value: value, name: key };
      } else {
        return {
          type: typeof value,
          value: `${key} is an unknown value`,
          name: `$$ERROR$$`,
        };
      }
  }
}

/**
 * This method takes an object and converts it to a string representation, with each
 * key-value pair on a new line and nested objects/arrays indented. The indentLevel
 * parameter specifies the initial indentation level, used to prettify the generated
 * eDSL from SeqJSON.
 * @param obj
 * @param indentLevel
 */
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
        //value is a Local or Parameter
        if (value instanceof Variable) {
          output += indent(`${key}: ${value.toReferenceString()}`, indentLevel) + ',\n';
        } else {
          output += indent(`${key}:{`, indentLevel) + '\n';
          indentLevel++;
          print(value);
          indentLevel--;
          output += indent(`},`, indentLevel) + '\n';
        }
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
