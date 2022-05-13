import * as AST from './windows-expressions-ast'
import { ActivityType, Resource, transition } from "./mission-model-generated-code";

export class WindowSet {
  public readonly __astnode: AST.WindowsExpression;
  private constructor(windowSpecifer: AST.WindowsExpression) {
    this.__astnode = windowSpecifer;
  }
  private static new(windowSpecifier: AST.WindowsExpression) {
    return new WindowSet(windowSpecifier);
  }

  public and(...others: WindowSet[]): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionAnd,
      windowsExpressions: [
        this.__astnode,
        ...others.map(other => other.__astnode),
      ],
    });
  }

  public or(...others: WindowSet[]): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionOr,
      windowsExpressions: [
        this.__astnode,
        ...others.map(other => other.__astnode),
      ],
    });
  }

  public static gt(resource: Resource, value: Double): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionGreaterThan,
      left: resource,
      right: value,
    });
  }

  public static lt(resource: Resource, value: Double): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionLessThan,
      left: resource,
      right: value,
    });
  }

  public static eq(resource: Resource, value: Double): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionEqualLinear,
      left: resource,
      right: value,
    });
  }

  public static neq(resource: Resource, value: Double): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionNotEqualLinear,
      left: resource,
      right: value,
    });
  }

  public static between(resource: Resource, lowerBound: Double, upperBound: Double): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionBetween,
      resource,
      lowerBound,
      upperBound
    });
  }

  public static transition(resource: Resource, from: any, to: any): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.WindowsExpressionTransition,
      resource,
      from,
      to
    });
  }

  public static during(activityType: ActivityType): WindowSet {
    return WindowSet.new({
      kind: AST.NodeKind.ActivityExpression,
      type: activityType
    })
  }
}

declare global {
  class WindowSet {
    public readonly __astnode: AST.WindowsExpression
    public and(...others: WindowSet[]): WindowSet
    public or(...others: WindowSet[]): WindowSet
    public static gt(resource: Resource, value: Double): WindowSet
    public static lt(resource: Resource, value: Double): WindowSet
    public static eq(resource: Resource, value: Double): WindowSet
    public static neq(resource: Resource, value: Double): WindowSet
    public static between(resource: Resource, lowerBound: Double, upperBound: Double): WindowSet
    public static transition: typeof transition
    public static during(activityType: ActivityType): WindowSet
  }
}

// Make Goal available on the global object
Object.assign(globalThis, { WindowSet });
