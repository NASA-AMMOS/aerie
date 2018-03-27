/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  RavenSource,
} from './../models';

export const rootSource: RavenSource = {
  actions: [],
  childIds: [],
  content: [],
  dbType: '',
  draggable: false,
  expandable: false,
  expanded: false,
  icon: '',
  id: '/',
  isServer: false,
  kind: '',
  label: 'root',
  menu: false,
  name: 'root',
  openable: false,
  opened: false,
  parentId: '',
  permissions: '',
  pinnable: false,
  pinned: false,
  selectable: false,
  selected: false,
  subBandIds: {},
  url: '',
};

export const childSource: RavenSource = {
  actions: [],
  childIds: [],
  content: [],
  dbType: '',
  draggable: false,
  expandable: true,
  expanded: false,
  icon: '',
  id: '/child',
  isServer: false,
  kind: 'db',
  label: 'test-child-source',
  menu: true,
  name: 'test-child-source',
  openable: false,
  opened: false,
  parentId: '0',
  permissions: '',
  pinnable: true,
  pinned: false,
  selectable: true,
  selected: false,
  subBandIds: {},
  url: '',
};
