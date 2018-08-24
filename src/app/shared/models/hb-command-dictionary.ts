/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

export interface HBCommandDictionary {
  /**
   * Unique identifier of the command dictionary
   */
  id: string;
  /**
   * Display name of the command dictionary
   */
  name: string;
  /**
   * Optional version of the command dictionary
   */
  version: string | null;
  /**
   * Whether the command dictionary is selected
   */
  selected: boolean;
}
