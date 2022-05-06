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
    public static during(activityType: ActivityType): WindowSet
  }
}

// Make Goal available on the global object
Object.assign(globalThis, { WindowSet });
