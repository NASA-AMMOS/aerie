/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenKeyByPipe } from './raven-key-by.pipe';

describe('RavenKeyByPipe', () => {
  it('create an instance', () => {
    const pipe = new RavenKeyByPipe();
    expect(pipe).toBeTruthy();
  });

  it('should properly return an object keyed by the arg', () => {
    const pipe = new RavenKeyByPipe();
    expect(
      pipe.transform([{ id: '0', val: 0 }, { id: '1', val: 1 }], 'id')
    ).toEqual({ '0': { id: '0', val: 0 }, '1': { id: '1', val: 1 } });
  });
});
