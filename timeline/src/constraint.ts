import type { Windows } from './profiles/windows.js';
import type { Interval } from './interval.js';

type Constraint = ConstraintResult | Windows;

export interface ConstraintResult {
  gaps: Interval[];
  violations: Violation[];
}

export interface Violation {
  intervals: Interval[];
  associatedActivityIds: number[];
}
