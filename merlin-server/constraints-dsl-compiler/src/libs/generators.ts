import {DirtyGeneratorError} from "./errors";

export function preparePlainGenerator<V>(gen: AsyncGenerator<V>): UncachedAsyncGenerator<V> | CachedAsyncGenerator<V> {
  if (getGeneratorType(gen) !== GeneratorType.Plain) return gen as UncachedAsyncGenerator<V> | CachedAsyncGenerator<V>;
  return {
    async next(...args: [] | [unknown]) {
      if (this.dirty) {
        throw new DirtyGeneratorError();
      }
      this.dirty = true;
      const result = await gen.next(args);
      this.next = gen.next;
      return result;
    },
    dirty: false,
    async throw(e: any)  { return await gen.throw(e); },
    async return(v: any) { return await gen.return(v); },
    [Symbol.asyncIterator]() { return this; }
  } as UncachedAsyncGenerator<V>;
}

export function cacheGenerator<V>(gen: UncachedAsyncGenerator<V> | CachedAsyncGenerator<V>): CachedAsyncGenerator<V> {
  if ('clone' in gen) return gen as CachedAsyncGenerator<V>;
  if (gen.dirty) {
    throw new Error("Cannot cache a generator that has already started. Call .clone() before collecting an aliased profile.");
  }

  let buffer: Promise<IteratorResult<V>>[] = [];

  return function make(n): CachedAsyncGenerator<V> {
    return {
      async next(...args: [] | [unknown]) {
        if (n >= buffer.length) buffer.push(gen.next(args));
        const result = await buffer[n++];
        if (result === undefined) {
          throw new Error("async generator value was undefined");
        }
        return result;
      },
      clone()   { return make(0); },
      async throw(e: any)  { return await gen.throw(e); },
      async return(v: any) { return await gen.return(v); },
      [Symbol.asyncIterator]() { return this; }
    };
  }(0);
}

export function cachePlainGenerator<V>(gen: AsyncGenerator<V>): CachedAsyncGenerator<V> {
  if (getGeneratorType(gen) !== GeneratorType.Plain) {
    throw new Error("generator was not plain");
  }
  return cacheGenerator(preparePlainGenerator(gen));
}

export type UncachedAsyncGenerator<V> = AsyncGenerator<V> & {
  dirty: boolean
};

export type CachedAsyncGenerator<V> = AsyncGenerator<V> & {
  clone(): CachedAsyncGenerator<V>
}

export enum GeneratorType {
  Cached,
  Uncached,
  Plain
}

export function getGeneratorType<V>(gen: AsyncGenerator<V>): GeneratorType {
  if ('dirty' in gen) return GeneratorType.Uncached;
  if ('clone' in gen) return GeneratorType.Cached;
  else return GeneratorType.Plain;
}

export async function collect<V>(gen: AsyncGenerator<V>): Promise<V[]> {
  let result: V[] = [];
  for await (const value of gen) {
    result.push(value);
  }
  return result;
}
