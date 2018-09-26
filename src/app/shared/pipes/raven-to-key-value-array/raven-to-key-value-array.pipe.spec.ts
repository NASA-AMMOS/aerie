/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenToKeyValueArrayPipe } from './raven-to-key-value-array.pipe';

describe('RavenToKeyValueArrayPipe', () => {
  it('create an instance', () => {
    const pipe = new RavenToKeyValueArrayPipe();
    expect(pipe).toBeTruthy();
  });

  it(`should return a 'key'/'value' array when passing no transform params`, () => {
    const pipe = new RavenToKeyValueArrayPipe();
    expect(pipe.transform({ a: 1 })).toEqual([{ key: 'a', value: 1 }]);
  });

  it(`should return a 'key'/'value' array when passing 'key' and 'y' as transform params`, () => {
    const pipe = new RavenToKeyValueArrayPipe();
    expect(pipe.transform({ a: 1 }, 'key', 'value')).toEqual([
      { key: 'a', value: 1 },
    ]);
  });

  it(`should return a 'x'/'y' array when passing 'x' and 'y' as transform params`, () => {
    const pipe = new RavenToKeyValueArrayPipe();
    expect(pipe.transform({ a: 1 }, 'x', 'y')).toEqual([{ x: 'a', y: 1 }]);
  });

  it(`should return 'key'/'value' array when passing an object with a nested object`, () => {
    const pipe = new RavenToKeyValueArrayPipe();
    const input = {
      mapping: {
        Events: {
          'Activity Name': 'event',
          'Draw Type': 'triangle',
          'Tend Assigned': 'Date',
          'Tstart Assigned': 'Date',
        },
      },
    };

    expect(pipe.transform(input)).toEqual([
      {
        key: 'mapping',
        value: {
          Events: {
            'Activity Name': 'event',
            'Draw Type': 'triangle',
            'Tend Assigned': 'Date',
            'Tstart Assigned': 'Date',
          },
        },
      },
    ]);
  });
});
