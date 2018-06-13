/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  uniqueId,
} from 'lodash';

import {
  RavenState,
} from './../models';

import {
  toCompositeBand,
} from './../util/bands';

/**
 * Takes a RavenState and does some transformations before saving it to the database:
 * 1. Remove ids.
 * 2. Flattens composite bands with one sub-band.
 */
export function exportState(state: RavenState): any {
  return {
    ...state,
    bands: state.bands
      // Remove all ids from bands and sub-bands.
      .map(band => {
        const { id: bandId, ...bandWithNoId } = band;

        return {
          ...bandWithNoId,
          subBands: band.subBands.map(subBand => {
            const { id: subBandId, parentUniqueId, ...subBandWithNoIds } = subBand;

            return {
              ...subBandWithNoIds,
            };
          }),
        };
      })
      // Flatten composite bands with a single sub-band.
      .reduce((bands: any[], band) => {
        if (band.subBands.length === 1) {
          bands.push({
            ...band.subBands[0],
            containerId: band.containerId,
            sortOrder: band.sortOrder,
          });
        } else {
          bands.push(band);
        }

        return bands;
      }, []),
  };
}

/**
 * Takes a saved state and transforms it into a RavenState:
 * 1. Creates composite bands from flattened sub-bands.
 * 2. Adds ids.
 */
export function importState(state: any): RavenState {
  return {
    ...state,
    bands: state.bands
      // Create composite bands.
      .reduce((bands: any[], band: any) => {
        if (band.type !== 'composite') {
          const { containerId, sortOrder, ...subBand } = band;
          bands.push(toCompositeBand(subBand, containerId, sortOrder));
        } else {
          bands.push(band);
        }

        return bands;
      }, [])
      // Add ids.
      .map((band: any) => {
        const parentUniqueId = uniqueId();

        return {
          ...band,
          id: parentUniqueId,
          subBands: band.subBands.map((subBand: any) => ({
            ...subBand,
            id: uniqueId(),
            parentUniqueId,
          })),
        };
      }),
  };
}
