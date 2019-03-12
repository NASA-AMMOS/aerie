/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { getParentSourceIds } from '../../shared/util';
import { SourceFilter } from './source-filter';

export interface FilterState {
  filter: SourceFilter;
  filteredSources: Set<string> | null; // `null` means "filter matches everything"
  visibleAncestors: Set<string> | null; // `null` means "filter matches everything"
}

// Note that a source should be considered an ancestor of itself (with zero
// separation), so `isAncestorOfMatch` returns true on strictly more things
// than does `isMatch.
export const FilterState = {
  empty(): FilterState {
    return {
      filter: SourceFilter.truth(),
      filteredSources: null,
      visibleAncestors: null,
    };
  },

  fromMatches(filter: SourceFilter, matches: string[]): FilterState {
    const filteredSources = new Set(matches);

    const visibleAncestors = new Set();
    for (const sourceId of matches) {
      visibleAncestors.add(sourceId);
      for (const ancestorId of getParentSourceIds(sourceId)) {
        visibleAncestors.add(ancestorId);
      }
    }

    return { filter, filteredSources, visibleAncestors };
  },

  isMatch(state: FilterState, source: string): boolean {
    return state.filteredSources === null || state.filteredSources.has(source);
  },

  isAncestorOfMatch(state: FilterState, source: string): boolean {
    return (
      state.visibleAncestors === null || state.visibleAncestors.has(source)
    );
  },
};
