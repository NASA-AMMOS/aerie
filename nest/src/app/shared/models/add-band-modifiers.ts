/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { BaseType, StringTMap } from '../../shared/models';

export interface AddBandModifiers {
  /**
   * Place the new band after the named band in the sort order.
   * Defaults to the end of the sort order.
   */
  afterBandId?: string;

  /**
   * A set of additional properties to include on subbands of this band.
   */
  additionalSubBandProps?: StringTMap<BaseType>;
}
