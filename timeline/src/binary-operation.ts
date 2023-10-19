import type { Interval } from './interval.js';

/** A generalized binary operation container for possibly undefined operands. */
export class BinaryOperation<Left, Right, Out> {
  private constructor(
    /** Left-operand-only operation for when the right operand is missing. */
    public readonly left: (l: Left, i: Interval) => Out | undefined,
    /** Right-operand-only operation for when the left operand is missing. */
    public readonly right: (r: Right, i: Interval) => Out | undefined,
    /** Binary operation for when both operands are present. */
    public readonly combine: (l: Left, r: Right, i: Interval) => Out | undefined
  ) {}

  /**
   * Constructs a binary operation from each case: left-only, right-only, and combining left and right.
   *
   * The interval on which the value exists is provided to the case functions along with the value.
   *
   * @param left Left-operand-only operation for when the right operand is missing.
   * @param right Right-operand-only operation for when the left operand is missing.
   * @param combine Binary operation for when both operands are present.
   */
  public static cases<Left, Right, Out>(
    left: (l: Left, i: Interval) => Out | undefined,
    right: (r: Right, i: Interval) => Out | undefined,
    combine: (l: Left, r: Right, i: Interval) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return new BinaryOperation<Left, Right, Out>(left, right, combine);
  }

  /**
   * Constructs a binary operation from a single function with possibly undefined arguments.
   *
   * The interval on which the values exist is provided to the function along with the values.
   *
   * @param func a function which computes the operation on possibly undefined left and right operands
   */
  public static singleFunction<Left, Right, Out>(
    func: (l: Left | undefined, r: Right | undefined, i: Interval) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return new BinaryOperation(
      (l, i) => func(l, undefined, i),
      (r, i) => func(undefined, r, i),
      (l, r, i) => func(l, r, i)
    );
  }

  /**
   * Constructs a binary operation from only the both-operand case. Defaults to `undefined` if either
   * operand is missing.
   *
   * @param func a function which computes the operation on both operands.
   */
  public static combineOrUndefined<Left, Right, Out>(
    func: (l: Left, r: Right, i: Interval) => Out | undefined
  ): BinaryOperation<Left, Right, Out> {
    return BinaryOperation.cases(
      _ => undefined,
      _ => undefined,
      (l, r, i) => func(l, r, i)
    );
  }

  /**
   * Constructs a binary operation from only the both-operand case. If either operand is missing, the
   * present operand will be passed through unchanged.
   *
   * The output type of the resulting operation is not just the return type of the provided function; it can
   * also be the left and right input types, in the case that any of these three types differ. For example,
   * if the operands are `number` and `string`, and the provided function returns `boolean`, then the overall
   * output type will be `boolean | number | string`.
   *
   * @param func a function which computes the operation on both operands.
   */
  public static combineOrIdentity<Left, Right, Combine>(
    func: (l: Left, r: Right, i: Interval) => Left | Right | Combine
  ): BinaryOperation<Left, Right, Left | Right | Combine> {
    return BinaryOperation.cases(
      l => l,
      r => r,
      (l, r, i) => func(l, r, i)
    );
  }
}
