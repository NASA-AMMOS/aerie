import type {Windows} from "./profiles/windows";
import type {Interval} from "./interval";

type Constraint = ConstraintResult | Windows;

export interface ConstraintResult {
  gaps: Interval[];
  violations: Violation[];
}

export interface Violation {
  intervals: Interval[];
  associatedActivityIds: number[];
}
