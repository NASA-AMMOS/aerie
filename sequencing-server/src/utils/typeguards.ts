/** Typeguard against a null value */
export function isNotNull<T>(entity: T | null): entity is T {
  return entity !== null;
}

/** Typeguard against an undefined value */
export function isDefined<T>(entity: T | undefined): entity is T {
  return entity !== undefined;
}

/** Typeguard resolved settled promises */
export function isResolved<T>(promise: PromiseSettledResult<T>): promise is PromiseFulfilledResult<T> {
  return promise.status === 'fulfilled';
}

/** Typeguard rejected settled promises */
export function isRejected<T>(promise: PromiseSettledResult<T>): promise is PromiseRejectedResult {
  return promise.status === 'rejected';
}
