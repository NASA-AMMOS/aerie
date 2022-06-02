// The following are only to satisfy the type checker before any code is generated.
// All of this will be overwritten before any constraints are evaluated.

export type ResourceName = string;
export type DiscreteResourceSchema<R extends ResourceName> = any;
export type RealResourceName = string;

export enum ActivityType {
  // This indicates to the compiler that we are using a string enum so we can assign it to string for our AST
  _ = '_',
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
}
