import type { Interval } from './interval';

export class BinaryOperation<Left, Right, Out> {
  public readonly left: (l: Left, i: Interval) => Out | undefined;
  public readonly right: (r: Right, i: Interval) => Out | undefined;
  public readonly combine: (l: Left, r: Right, i: Interval) => Out | undefined;

  private constructor(
    left: (l: Left, i: Interval) => Out | undefined,
    right: (r: Right, i: Interval) => Out | undefined,
    combine: (l: Left, r: Right, i: Interval) => Out | undefined
  ) {
    this.left = left;
    this.right = right;
    this.combine = combine;
  }

  public static cases<Left, Right, Out>(
    left: (l: Left, i: Interval) => Out | undefined,
    right: (r: Right, i: Interval) => Out | undefined,
    combine: (l: Left, r: Right, i: Interval) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return new BinaryOperation<Left, Right, Out>(left, right, combine);
  }

  public static singleFunction<Left, Right, Out>(
    func: (l: Left | undefined, r: Right | undefined, i: Interval) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return new BinaryOperation(
      (l, i) => func(l, undefined, i),
      (r, i) => func(undefined, r, i),
      (l, r, i) => func(l, r, i)
    );
  }

  public static combineOrUndefined<Left, Right, Out>(
    func: (l: Left, r: Right, i: Interval) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return BinaryOperation.cases(
      _ => undefined,
      _ => undefined,
      (l, r, i) => func(l, r, i)
    );
  }

  public static combineOrIdentity<V>(func: (l: V, r: V, i: Interval) => V): BinaryOperation<V, V, V> {
    return BinaryOperation.cases(
      l => l,
      r => r,
      (l, r, i) => func(l, r, i)
    );
  }
}
