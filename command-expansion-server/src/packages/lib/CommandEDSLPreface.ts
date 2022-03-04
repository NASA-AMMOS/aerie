/** START Preface */
type ArgType = boolean | string | number;
export class Command<A extends ArgType[] | {[argName: string]: any} = [] | {}> {
  public readonly stem: string;
  public readonly arguments: A;

  private constructor(opts: {
    stem: string
    arguments: A,
  }) {
    this.stem = opts.stem;
    this.arguments = opts.arguments
  }

  public static new<A extends any[] | {[argName: string]: any}>(opts: {
    stem: string
    arguments: A,
  }): Command<A> {
    return new Command({
      stem: opts.stem,
      arguments: opts.arguments,
    });
  }

  public toSeqJson() {
    return {
      id: 'command',
      metadata: {},
      steps: [
        {
          stem: this.stem,
          time: {type: 'COMPLETE'},
          type: 'command',
          metadata: {},
          args: typeof this.arguments == 'object' ? Object.values(this.arguments) : this.arguments,
        }
      ]
    }
  }
}
type Arrayable<T> = T | Arrayable<T>[];

export type ExpansionReturn = Arrayable<Command>;

declare global {
  type Context = {}

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
