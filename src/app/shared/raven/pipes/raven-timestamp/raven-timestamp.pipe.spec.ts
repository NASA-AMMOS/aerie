/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenTimestampPipe } from './raven-timestamp.pipe';

describe('RavenTimestampPipe', () => {
  it('create an instance', () => {
    const pipe = new RavenTimestampPipe();
    expect(pipe).toBeTruthy();
  });

  it('should return a correct timestamp for the given time', () => {
    const pipe = new RavenTimestampPipe();
    expect(pipe.transform(1667498617)).toBe('2022-307T18:03:37.000');
    expect(pipe.transform(1678662802.685)).toBe('2023-071T23:13:22.685');
  });
});
