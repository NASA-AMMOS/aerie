/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

/**
 * A rudimentary interface for activity instances
 */
export interface RavenActivity {
  /**
   * ID of the parent activity type
   */
  activityTypeId: string;

  /**
   * Duration of activity
   */
  duration: string;

  /**
   * Id of the activity
   */
  id: string;

  /**
   * Description of the activity
   */
  intent: string;

  /**
   * Name of activity
   */
  name: string;

  /**
   * ID of the associated sequence for this activity
   */
  sequenceId: string;

  /**
   * Start time of activity
   */
  start: string;
}
