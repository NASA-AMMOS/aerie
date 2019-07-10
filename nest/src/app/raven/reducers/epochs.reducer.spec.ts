/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AddEpochs,
  AppendAndReplaceEpochs,
  RemoveEpochs,
  UpdateEpochData,
  UpdateEpochSetting,
} from '../actions/epochs.actions';
import { EpochsState, initialState, reducer } from './epochs.reducer';

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
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ]),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ],
    });
  });

  it('handle UpdateEpochSetting dayCode', () => {
    epochsState = reducer(
      epochsState,
      new UpdateEpochSetting({ dayCode: 'D' }),
    );
    expect(epochsState).toEqual({
      ...initialState,
      dayCode: 'D',
    });
  });

  it('handle UpdateEpochSetting earthSecToEpochSec', () => {
    epochsState = reducer(
      epochsState,
      new UpdateEpochSetting({ earthSecToEpochSec: 1.16 }),
    );
    expect(epochsState).toEqual({
      ...initialState,
      earthSecToEpochSec: 1.16,
    });
  });

  it('handle UpdateEpochSetting inUseEpoch', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new UpdateEpochSetting({
        inUseEpoch: {
          name: 'Day2',
          selected: true,
          value: '2018-156T23:00:00.000',
        },
      }),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ],
      inUseEpoch: {
        name: 'Day2',
        selected: true,
        value: '2018-156T23:00:00.000',
      },
    });
  });

  it('handle AddEpochs abd AppendAndReplaceEpochs', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new AppendAndReplaceEpochs([
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ]),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ],
      modified: true,
    });
  });

  it('handle AddEpochs(AppendAndreplace)', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new AppendAndReplaceEpochs([
        { name: 'Day1', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ]),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day1', value: '2019-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ],
      modified: true,
    });
  });

  it('handle AddEpochs(RemoveAll)', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ]),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ],
      modified: true,
    });
  });

  it('handle RemoveEpochs', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new AppendAndReplaceEpochs([
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new RemoveEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ]),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
      ],
      modified: true,
    });
  });

  it('handle UpdateEpochData', () => {
    epochsState = reducer(
      epochsState,
      new AddEpochs([
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Day2', value: '2018-156T23:00:00.000', selected: false },
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ]),
    );
    epochsState = reducer(
      epochsState,
      new UpdateEpochData(1, {
        name: 'Epoch1',
        selected: false,
        value: '2023-123T12:00:00',
      }),
    );
    expect(epochsState).toEqual({
      ...initialState,
      epochs: [
        { name: 'Day1', value: '2018-134T12:00:00.000', selected: false },
        { name: 'Epoch1', value: '2023-123T12:00:00', selected: false },
        { name: 'DayA', value: '2019-134T12:00:00.000', selected: false },
        { name: 'DayB', value: '2019-156T23:00:00.000', selected: false },
      ],
      modified: true,
    });
  });
});
