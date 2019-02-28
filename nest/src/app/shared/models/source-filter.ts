/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

export type StringPredicate = MatchesPredicate;
export interface MatchesPredicate {
  matches: string;
}

export interface TrueSourceFilter {}
export interface NameSourceFilter {
  name: StringPredicate;
}

export type SourceFilter = TrueSourceFilter | NameSourceFilter;

export const SourceFilter = {
  // A filter is the empty (vacuously true) filter if it's the empty object {}.
  // `null` is a synonym for this filter.
  isEmpty(filter: SourceFilter): boolean {
    return Object.keys(filter).length === 0;
  },

  truth(): TrueSourceFilter {
    return {};
  },
};
