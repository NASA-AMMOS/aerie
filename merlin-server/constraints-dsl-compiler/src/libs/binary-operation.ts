export class BinaryOperation<Left, Right, Out> {
  public readonly left: (l: Left) => Out | undefined;
  public readonly right: (r: Right) => Out | undefined;
  public readonly combine: (l: Left, r: Right) => Out | undefined;

  private constructor(
      left: (l: Left) => Out | undefined,
      right: (r: Right) => Out | undefined,
      combine: (l: Left, r: Right) => Out | undefined
  ) {
    this.left = left;
    this.right = right;
    this.combine = combine;
  }

  public static cases<Left, Right, Out>(
      left: (l: Left) => Out | undefined,
      right: (r: Right) => Out | undefined,
      combine: (l: Left, r: Right) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return new BinaryOperation<Left, Right, Out>(left, right, combine);
  }

  public static singleFunction<Left, Right, Out>(
      func: (l: Left | undefined, r: Right | undefined) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return new BinaryOperation(
        l => func(l, undefined),
        r => func(undefined, r),
        (l, r) => func(l, r)
    );
  }

  public static combineOrUndefined<Left, Right, Out>(
      func: (l: Left, r: Right) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return BinaryOperation.cases(
        _ => undefined,
        _ => undefined,
        (l, r) => func(l, r)
    );
  }

  public static combineOrIdentity<Left, Right, Out>(
      func: (l: Left, r: Right) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return BinaryOperation.cases(
        l => l,
        r => r,
        (l, r) => func(l, r)
    );
  }
}

export enum OpMode {
  Left,
  Right,
  Combine
}
