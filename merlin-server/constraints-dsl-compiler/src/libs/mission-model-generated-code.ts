// The following are only to satisfy the type checker before any code is generated.
// All of this will be overwritten before any constraints are evaluated.

export type ResourceName = string;
export type DiscreteResourceSchema<R extends ResourceName> = any;
export type RealResourceName = string;

export type ActivityTypeName = string;
export type ActivityParameters<A extends ActivityTypeName> = any;
export class ActivityInstance<A extends ActivityTypeName> {
  private readonly __activityType: A;
  private readonly __alias: string;

  public readonly parameters: ActivityParameters<A>;
  constructor(activityType: A, alias: string) {
    this.__activityType = activityType;
    this.__alias = alias;
    this.parameters = 5;
  }
}
