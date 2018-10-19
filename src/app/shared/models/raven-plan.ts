/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

/**
 * A rudimentary interface for activity types
 */
export interface RavenPlan {
  /**
   * Id of the plan
   */
  id: string;

  /**
   * Name of plan
   */
  name: string;

  /**
   * When the plan should start
   */
  start: string;

  /**
   * When the plan should end
   */
  end: string;

  /**
   * Minimum state of charge
   */
  msoc: number;

  /**
   * Handover state of charge
   */
  hsoc: number;

  /**
   * Max power load
   */
  mpow: number;
}
