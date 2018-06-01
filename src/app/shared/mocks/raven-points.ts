/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  RavenActivityPoint,
} from './../models';

export const activityPoint: RavenActivityPoint = {
  activityId: '',
  activityName: 'test-activity-point',
  activityParameters: [
    { Name: '', Value: '' },
  ],
  activityType: '',
  ancestors: [],
  arguments: 0,
  childrenUrl: '',
  color: [
    0, 0, 0,
  ],
  descendantsUrl: '',
  duration: 10,
  end: 500,
  endTimestamp: '',
  id: '400',
  keywordLine: '',
  legend: '',
  message: null,
  metadata: [],
  sourceId: '/a/b/c/',
  start: 0,
  startTimestamp: '',
  subBandId: '4',
  type: 'activity',
  uniqueId: '400',
};

export const messageTypeActivityPoint: RavenActivityPoint = {
  activityId: '',
  activityName: 'test-activity-point',
  activityParameters: [
    { Name: '', Value: '' },
  ],
  activityType: '',
  ancestors: [],
  arguments: 0,
  childrenUrl: '',
  color: [
    0, 0, 0,
  ],
  descendantsUrl: '',
  duration: 10,
  end: 500,
  endTimestamp: '',
  id: '400',
  keywordLine: '',
  legend: '',
  message: 'warning message',
  metadata: [],
  sourceId: '/a/b/c/',
  start: 0,
  startTimestamp: '',
  subBandId: '4',
  type: 'activity',
  uniqueId: '400',
};

export const keywordLineActivityPoint: RavenActivityPoint = {
  activityId: '',
  activityName: 'test-activity-point',
  activityParameters: [
    { Name: '', Value: '' },
  ],
  activityType: '',
  ancestors: [],
  arguments: 0,
  childrenUrl: '',
  color: [
    0, 0, 0,
  ],
  descendantsUrl: '',
  duration: 10,
  end: 500,
  endTimestamp: '',
  id: '400',
  keywordLine: 'some keyword',
  legend: '',
  message: 'some message',
  metadata: [],
  sourceId: '/a/b/c/',
  start: 0,
  startTimestamp: '',
  subBandId: '4',
  type: 'activity',
  uniqueId: '400',
};
