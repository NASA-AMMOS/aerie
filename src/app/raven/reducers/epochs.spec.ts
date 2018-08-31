/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { EpochsState, initialState, reducer } from './epochs';

import { AddEpochs, UpdateEpochs } from '../actions/epochs';

describe('epochs reducer', () => {
  let epochsState: EpochsState;

  beforeEach(() => {
    epochsState = initialState;
  });

  it('handle default', () => {
    expect(epochsState).toEqual(initialState);
  });

  it('handle AddEpochs', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000' },
        { name: 'Day2', value: '2018-156T23:00:00.000' },
      ])
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000' },
        { name: 'Day2', value: '2018-156T23:00:00.000' },
      ],
    });
  });

  it('handle UpdateEpochs dayCode', () => {
    epochsState = reducer(epochsState, new UpdateEpochs({ dayCode: 'D' }));
    expect(epochsState).toEqual({
      ...initialState,
      dayCode: 'D',
    });
  });

  it('handle UpdateEpochs earthSecToEpochSec', () => {
    epochsState = reducer(
      epochsState,
      new UpdateEpochs({ earthSecToEpochSec: 1.16 })
    );
    expect(epochsState).toEqual({
      ...initialState,
      earthSecToEpochSec: 1.16,
    });
  });

  it('handle UpdateEpochs inUseEpoch', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000' },
        { name: 'Day2', value: '2018-156T23:00:00.000' },
      ])
    );
    epochsState = reducer(
      epochsState,
      new UpdateEpochs({
        inUseEpoch: { name: 'Day2', value: '2018-156T23:00:00.000' },
      })
    );
    expect(epochsState).toEqual({
      ...initialState,
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
