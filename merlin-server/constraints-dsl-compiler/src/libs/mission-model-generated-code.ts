// The following are only to satisfy the type checker before any code is generated.
// All of this will be overwritten before any constraints are evaluated.

export type ActivityTypeName = string;
export type ResourceName = string;
export type DiscreteResourceSchema<R extends ResourceName> = any;
export function discreteResourceSchemaDummyValue<R extends ResourceName>(resource: R): DiscreteResourceSchema<R> {
  return 5;
}
export type RealResourceName = string;
