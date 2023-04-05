// The following are only to satisfy the type checker before any code is generated.
// All of this will be overwritten before any constraints are evaluated.

import { Discrete } from "./constraints-edsl-fluent-api.js";
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

export const ActivityTypeParameterInstantiationMap = {
  [ActivityType.activity]: (alias: string) => ({
    parameter: new Discrete<string>({
      kind: AST.NodeKind.DiscreteProfileParameter,
      alias,
      name: 'parameter'
    })
  }),
}

export type ParameterTypeactivity = {
  parameter: string
}
export type ActivityTypeParameterMap = {
  [ActivityType.activity]: ParameterTypeactivity
};

//same as above but parameters can be undefined
export type ParameterTypeWithUndefinedactivity = {
  parameter: string | undefined
}
export type ActivityTypeParameterMapWithUndefined = {
  [ActivityType.activity]: ParameterTypeWithUndefinedactivity
};
