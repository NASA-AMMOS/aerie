/**
 *
 * This is just a placeholder to placate the type checker for our node process.
 * At runtime, this file is replaced with the real generated code.
 *
 */

import {Windows} from "./constraints-edsl-fluent-api";

export enum ActivityType {
  // This indicates to the compiler that we are using a string enum so we can assign it to string for our AST
  _ = '_',
}

export enum Resource {
  "/abc" = "/abc",
  "/abc/def" = "/abc/def"
}

type ResourceUnion =
    | "/abc"
    | "/abc/def"

export function transition<T extends ResourceUnion, I =
          T extends "/abc" ? Double
        : T extends "/abc/def" ? string
        : never
>(resource: T, from: I, to: I): Windows {
  throw new Error("This function exists for typechecking purposes only");
}
