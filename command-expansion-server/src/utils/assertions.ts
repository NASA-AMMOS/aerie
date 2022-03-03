
/** Assertion error that has the associated object being asserted attached */
export class AssertionError<T> extends Error {
  // tslint:disable-next-line:no-any
  public context?: T;
  public timestamp: number;
  public other?: { [key: string]: any };
  // tslint:disable-next-line:no-any
  constructor(args: { message: string; context?: T; }) {
    const context = args.context;
    const timestamp = Date.now();

    super(args.message);
    this.context = context;
    this.timestamp = timestamp;
  }
}

/** Assert only one in an array and return that one */
export function assertOne<T>(entities: { [key: number]: T | undefined | null; length: number }, message?: string, context?: any): T {
  if (entities.length !== 1) {
    throw new AssertionError({
      message: message ?? `Expected only one, but received ${entities.length}. There may be a malformation issue.`,
      context: context ?? entities,
    });
  }
  return entities[0] as T;
}

/** Assert only one in an array and return that one or null */
export function assertOneOrUndefined<T>(
  entities: { [key: number]: T | undefined | null; length: number },
  message?: string,
  context?: any,
): T | undefined {
  if (entities.length > 1) {
    throw new AssertionError({
      message: message ?? `Expected at most one, but received ${entities.length}. There may be a malformation issue.`,
      context: context ?? entities,
    });
  }
  if (entities.length === 0) {
    return undefined;
  }
  const singleEntity = entities[0];
  if (singleEntity === null) {
    return undefined;
  }
  return singleEntity;
}

/** Assert that a thing is defined */
export function assertDefined<T>(entity: T | undefined | null, message?: string, context?: any): T {
  // tslint:disable-next-line:no-null-keyword
  if (entity === undefined || entity === null) {
    throw new AssertionError({
      message: message ?? `Expected item to be defined, but was undefined.`,
      context: context ?? entity,
    });
  }
  return entity;
}

/** Assert that an entity is a String */
export function assertString<T>(entity: T | string, message?: string, context?: any): string {
  if (typeof entity !== 'string') {
    throw new AssertionError({
      message: message ?? `Expected item to be a string, but was ${typeof entity}.`,
      context: context ?? entity,
    });
  }
  return entity;
}

/** Assert that an entity is a Date */
export function assertDate<T>(entity: T | Date, message?: string, context?: any): Date {
  if (!(entity instanceof Date)) {
    throw new AssertionError({
      message: message ?? `Expected item to be a Date, but was not.`,
      context: context ?? entity,
    });
  }
  return entity;
}

/** Assert that an entity is a Number */
export function assertNumber<T>(entity: T | number, message?: string, context?: any): number {
  if (typeof entity !== 'number') {
    throw new AssertionError({
      message: message ?? `Expected item to be a number, but was ${typeof entity}.`,
      context: context ?? entity,
    });
  }
  return entity;
}

/** Assert that an entity is a Boolean */
export function assertBoolean<T>(entity: T | boolean, message?: string, context?: any): boolean {
  if (typeof entity !== 'boolean') {
    throw new AssertionError({
      message: message ?? `Expected item to be a boolean, but was ${typeof entity}.`,
      context: context ?? entity,
    });
  }
  return entity;
}

/** Assert that an entity is one of the specified types */
export function assertTypes<T>(entity: any, types: string[], message?: string, context?: any): T {
  if (!types.includes(typeof entity)) {
    throw new AssertionError({
      message: message ?? `Expected item to be one of types ${types}, but was ${typeof entity}.`,
      context: context ?? entity,
    });
  }
  return entity;
}

/** Assert that an environment variable is defined */
export function assertEnvironmentVariable(
  environmentVariable: string,
  message?: string,
  isSecret: boolean = false,
  silent: boolean = false,
): string {
  const environmentVariableValue = process.env[environmentVariable];

  if (environmentVariableValue === undefined || environmentVariableValue === null) {
    throw new AssertionError({
      message: message ?? `Environment variable ${environmentVariable} must be set`,
      context: { environmentVariables: Object.keys(process.env) },
    });
  }
  if (!silent) {
    console.info(`Environment Variable Set: ${environmentVariable}=${isSecret ? '**********' : environmentVariableValue}`);
  }
  return environmentVariableValue;
}

/** Assert an expression */
export function assert(expression: boolean, message?: string, context?: any) {
  if (!expression) {
    throw new AssertionError({
      message: message ?? `Assertion failed`,
      context: context ?? expression,
    });
  }
}
