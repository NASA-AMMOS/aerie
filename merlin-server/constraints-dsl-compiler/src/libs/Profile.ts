import {Segment} from "./Segment";
import {Interval} from "./interval";
import {Windows} from "./windows";
import {
  CachedAsyncGenerator,
  collect,
  GeneratorType,
  getGeneratorType,
  cacheGenerator, preparePlainGenerator,
  UncachedAsyncGenerator
} from "./generators";

export enum ProfileType {
  Real,
  Discrete,
  Windows,
  Other
}

export class Profile<V> {
  private segments: UncachedAsyncGenerator<Segment<V>> | CachedAsyncGenerator<Segment<V>>;
  private readonly type_tag: ProfileType;

  constructor(segments: AsyncGenerator<Segment<V>>, type_tag: ProfileType) {
    this.segments = preparePlainGenerator(segments);
    this.type_tag = type_tag;
  }

  public static from<V>(data: Segment<V>[], type_tag: ProfileType): ProfileSpecialization<V>;
  public static from<V>(data: Segment<V>, type_tag: ProfileType): ProfileSpecialization<V>;
  public static from<V>(data: V, type_tag: ProfileType): ProfileSpecialization<V>;
  public static from<V>(data: any, type_tag: ProfileType): ProfileSpecialization<V> {
    let segments: AsyncGenerator<Segment<V>>;
    if (Array.isArray(data)) {
      segments = (async function* () {
        for (const segment of data) {
          yield segment;
        }
      })();
    } else if (data instanceof Segment) {
      segments = (async function* () {
        yield data;
      })();
    } else {
      segments = (async function* () {
        yield new Segment(Interval.Forever, data);
      })();
    }
    return (new Profile(segments, type_tag)).specialize();
  }

  public map(f: (v: Segment<V>) => Segment<V>): ProfileSpecialization<V>;
  public map<W>(f: (v: Segment<V>) => Segment<W>, type_tag: ProfileType): ProfileSpecialization<W>;
  public map<W>(f: (v: Segment<V>) => Segment<W>, type_tag?: ProfileType): ProfileSpecialization<W> {
    let outer_segments = this.segments;
    if (type_tag === undefined) {
      type_tag = this.type_tag;
    }
    let result = new Profile<W>((async function*()  {
      for await (const s of outer_segments) {
        yield f(s);
      }
    })(), type_tag);

    return result.specialize();
  }

  public map_values(f: (v: Segment<V>) => V): ProfileSpecialization<V>;
  public map_values<W>(f: (v: Segment<V>) => W, type_tag: ProfileType): ProfileSpecialization<W>;
  public map_values<W>(f: (v: Segment<V>) => W, type_tag?: ProfileType): ProfileSpecialization<W> {
    return this.map(
        (s) => ({value: f(s), interval: s.interval}),
        type_tag !== undefined ? type_tag : this.type_tag
    );
  }

  public map_intervals(f: (s: Segment<V>) => Interval): ProfileSpecialization<V> {
    return this.map(
        (s) => ({value: s.value, interval: f(s)})
    )
  }

  public filter(f: (s: Segment<V>) => boolean): ProfileSpecialization<V> {
    let outer_segments = this.segments;
    let result = new Profile<V>((async function*()  {
      for await (const s of outer_segments) {
        if (f(s)) {
          yield s;
        }
      }
    })(), this.type_tag);

    return result.specialize();
  }

  public clone(n: number): ProfileSpecialization<V> {
    if (getGeneratorType(this.segments) === GeneratorType.Uncached) {
      this.segments = cacheGenerator(this.segments as UncachedAsyncGenerator<Segment<V>>);
    }

    // @ts-ignore
    return (new Profile<V>(this.segments.clone(), this.type_tag)).specialize();
  }

  public async collect(): Promise<Segment<V>[]> {
    return await collect(this.segments);
  }

  public select(interval: Interval): ProfileSpecialization<V> {
    return this.specialize();
  }

  public specialize(): ProfileSpecialization<V> {
    if (this.type_tag === ProfileType.Windows) {
      // @ts-ignore
      return new Windows(this);
    } else if (this.type_tag === ProfileType.Real) {
      // @ts-ignore
      return new Real(this);
    }

    // @ts-ignore
    return this;
  }
}

type ProfileSpecialization<V> =
  V extends boolean ? Windows
  : Profile<V>;
