// The following are only to satisfy the type checker before any code is generated.
// All of this will be overwritten before any constraints are evaluated.

import { Windows, Discrete } from "./constraints-edsl-fluent-api.js";
import * as AST from "./constraints-ast.js";

export type Resource = {
  "my discrete resource": string,
  "my real resource": number
};
export type ResourceName = "my discrete resource" | "my real resource";
export type RealResourceName = "my real resource";

export enum ActivityType {
  // This indicates to the compiler that we are using a string enum so we can assign it to string for our AST
  activity = 'activity',
}

export const ActivityTypeParameterMap = {
  [ActivityType.activity]: (alias: string) => ({
    parameter: new Discrete<string>({
      kind: AST.NodeKind.DiscreteProfileParameter,
      alias,
      name: 'parameter'
    })
  }),
}

export type ActivityParameters<A extends ActivityType> = any;
export class ActivityInstance<A extends ActivityType> {
  private readonly __activityType: A;
  private readonly __alias: string;

  public readonly parameters: ActivityParameters<A>;
  constructor(activityType: A, alias: string) {
    this.__activityType = activityType;
    this.__alias = alias;
    this.parameters = 5;
  }

  public window(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionActivityWindow,
      alias: this.__alias
    });
  }
}
