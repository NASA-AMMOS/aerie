/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MpsServerSituationalAwarenessPefEntry } from '../models';
import { RavenSituationalAwarenessPefEntry } from '../models';

/**
 * Transform an array of MPS Server situationalAwareness pef entries
 * to Raven situationalAwareness pef entries.
 */
export function toRavenPefEntries(
  mpsServerPefEntries: MpsServerSituationalAwarenessPefEntry[],
): RavenSituationalAwarenessPefEntry[] {
  const ravenPefEntries: RavenSituationalAwarenessPefEntry[] = [];

  for (let i = 0, l = mpsServerPefEntries.length; i < l; ++i) {
    const serverSituationalAwarenessEntry: MpsServerSituationalAwarenessPefEntry =
      mpsServerPefEntries[i];

    ravenPefEntries.push({
      endTime: serverSituationalAwarenessEntry.pefEndTime,
      pefFile: serverSituationalAwarenessEntry.pefCollection,
      sequenceId: serverSituationalAwarenessEntry.sequenceId,
      startTime: serverSituationalAwarenessEntry.pefStartTime,
    });
  }

  return ravenPefEntries;
}
