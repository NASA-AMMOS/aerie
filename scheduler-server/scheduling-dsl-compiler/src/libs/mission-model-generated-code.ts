/**
 *
 * This is just a placeholder to placate the type checker for our node process.
 * At runtime, this file is replaced with the real generated code.
 *
 */

import {WindowSet} from "./windows-edsl-fluent-api";

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

export function transition(resource: "/abc", from: any, to: any): WindowSet
export function transition(resource: "/abc/def", from: any, to: any): WindowSet
export function transition(resource: ResourceUnion, from: any, to: any): WindowSet {
  throw new Error("This function exists for typechecking purposes only");
}
