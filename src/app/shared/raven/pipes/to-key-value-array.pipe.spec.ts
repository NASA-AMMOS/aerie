/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ToKeyValueArrayPipe } from './to-key-value-array.pipe';

describe('ToKeyValueArrayPipe', () => {
  it('create an instance', () => {
    const pipe = new ToKeyValueArrayPipe();
    expect(pipe).toBeTruthy();
  });

  it(`should return a 'key'/'value' array when passing no transform params`, () => {
    const pipe = new ToKeyValueArrayPipe();
    expect(pipe.transform({ a: 1 })).toEqual([{ key: 'a', value: 1 }]);
  });

  it(`should return a 'key'/'value' array when passing 'key' and 'y' as transform params`, () => {
    const pipe = new ToKeyValueArrayPipe();
    expect(pipe.transform({ a: 1 }, 'key', 'value')).toEqual([{ key: 'a', value: 1 }]);
  });

  it(`should return a 'x'/'y' array when passing 'x' and 'y' as transform params`, () => {
    const pipe = new ToKeyValueArrayPipe();
    expect(pipe.transform({ a: 1 }, 'x', 'y')).toEqual([{ x: 'a', y: 1 }]);
  });

  it(`should return 'key'/'value' array when passing an object with a nested object`, () => {
    const pipe = new ToKeyValueArrayPipe();
    const input = { mapping: { Events: { 'Tstart Assigned': 'Date', 'Tend Assigned': 'Date', 'Activity Name': 'event', 'Draw Type': 'triangle' } } };

    expect(pipe.transform(input)).toEqual([{
      key: 'mapping',
      value: { Events: { 'Tstart Assigned': 'Date', 'Tend Assigned': 'Date', 'Activity Name': 'event', 'Draw Type': 'triangle' } },
    }]);
  });
});
