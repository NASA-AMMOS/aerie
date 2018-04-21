/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  EpochsState,
  initialState,
  reducer,
} from './epochs';

import {
  AddEpochs,
  ChangeDayCode,
  ChangeEarthSecToEpochSec,
  SelectEpoch,
} from './../actions/epochs';

describe('epochs reducer', () => {
  let epochsState: EpochsState;

  beforeEach(() => {
    epochsState = initialState;
  });

  it('handle default', () => {
    expect(epochsState).toEqual(initialState);
  });

  it('handle AddEpochs', () => {
    epochsState = reducer(epochsState, new AddEpochs([
      { name: 'Day1', value: '2018-134T12:00:00.000' },
      { name: 'Day2', value: '2018-156T23:00:00.000' },
    ]));
    expect(epochsState).toEqual({
      ...initialState,
      dayCode: '',
      earthSecToEpochSec: 1,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000' },
        { name: 'Day2', value: '2018-156T23:00:00.000' },
      ],
      inUseEpoch: null,
    });
  });

  it('handle ChangeDayCode', () => {
    epochsState = reducer(epochsState, new ChangeDayCode('D'));
    expect(epochsState).toEqual({
      ...initialState,
      dayCode: 'D',
      earthSecToEpochSec: 1,
      epochs: [],
      inUseEpoch: null,
    });
  });

  it('handle ChangeEarthSecToEpochSec', () => {
    epochsState = reducer(epochsState, new ChangeEarthSecToEpochSec(1.16));
    expect(epochsState).toEqual({
      ...initialState,
      dayCode: '',
      earthSecToEpochSec: 1.16,
      epochs: [],
      inUseEpoch: null,
    });
  });

  it('handle SelectEpoch', () => {
    epochsState = reducer(epochsState, new AddEpochs([
      { name: 'Day1', value: '2018-134T12:00:00.000' },
      { name: 'Day2', value: '2018-156T23:00:00.000' },
    ]));
    epochsState = reducer(epochsState, new SelectEpoch({ name: 'Day2', value: '2018-156T23:00:00.000' }));
    expect(epochsState).toEqual({
      ...initialState,
      dayCode: '',
      earthSecToEpochSec: 1,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000' },
        { name: 'Day2', value: '2018-156T23:00:00.000' },
      ],
      inUseEpoch: {
        name: 'Day2',
        value: '2018-156T23:00:00.000',
      },
    });
  });
});
