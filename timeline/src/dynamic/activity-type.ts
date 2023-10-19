export const ActivityType = new Proxy(
  {},
  {
    get: (_target, prop, _receiver) => {
      if (typeof prop === 'symbol') throw new Error("cannot access a symbol property of the ActivityType proxy");

      return prop as string;
    }
  }
);
