export const AnyActivityType = Symbol('AnyActivityType');

export const ActivityType = new Proxy(
  {},
  {
    get: () => AnyActivityType
  }
);

export type ActivityTypeName = typeof AnyActivityType;

export type ActivityTypeParameterMap = {
  [AnyActivityType]: {
    [param: string]: any;
  };
};
