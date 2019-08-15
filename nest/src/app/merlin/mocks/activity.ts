/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ActivityInstance } from '../../shared/models';

export const activity: ActivityInstance = {
  activityId: '9e8f7d40-01b3-4e27-b7e5-a9661ba8aa07',
  activityType: 'PeelBanana',
  backgroundColor: '#ffffff',
  constraints: [],
  duration: 2.66899991035461,
  end: 1656459369.638,
  endTimestamp: '2022-179T23:36:09.638',
  intent: '...',
  listeners: [],
  name: 'Tracking_FIXED',
  parameters: [
    {
      name: 'SomeStringParam',
      type: 'String',
      value: 'ON',
    },
    {
      name: 'SomeNumberParam',
      type: 'Double',
      value: '42',
    },
    {
      name: 'SomeBooleanParam',
      type: 'Boolean',
      value: 'true',
    },
    {
      name: 'SomeEnumParam',
      type: 'Enum',
      value: 'LOW',
    },
  ],
  start: 1656459366.969,
  startTimestamp: '2022-179T23:36:06.969',
  textColor: '#000000',
  y: 138.0,
};
