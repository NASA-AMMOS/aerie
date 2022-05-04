import * as AST from './windows-expressions-ast'
import { ActivityType, Resource } from "./mission-model-generated-code";

export class WindowSet {
  public readonly __astnode: AST.WindowsExpression;
  private constructor(windowSpecifer: AST.WindowsExpression) {
    this.__astnode = windowSpecifer;
  }
  private static new(windowSpecifier: AST.WindowsExpression) {
    return new WindowSet(windowSpecifier);
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
    public static gt(resource: Resource, value: Double): WindowSet
    public static lt(resource: Resource, value: Double): WindowSet
    public static during(activityType: ActivityType): WindowSet
  }
}

// Make Goal available on the global object
Object.assign(globalThis, { WindowSet });
