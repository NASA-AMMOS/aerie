/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenDurationPipe } from './raven-duration.pipe';

describe('RavenDurationPipe', () => {
  it('create an instance', () => {
    const pipe = new RavenDurationPipe();
    expect(pipe).toBeTruthy();
  });

  it('should properly return a correct T format duration for the given duration', () => {
    const pipe = new RavenDurationPipe();
    expect(pipe.transform(86400)).toBe('001T00:00:00');
  });
});
