/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  MpsServerSourceGraphable,
  RavenFileMetadata,
  RavenSourceAction,
} from './index';

export interface RavenSource {
  actions: RavenSourceAction[];
  childIds: string[];
  content: MpsServerSourceGraphable[] | null;
  dbType: string;
  draggable: boolean;
  expandable: boolean;
  expanded: boolean;
  fileMetadata: RavenFileMetadata;
  icon: string;
  id: string;
  isServer: boolean;
  kind: string;
  label: string;
  menu: boolean;
  name: string;
  openable: boolean;
  opened: boolean;
  parentId: string;
  pinnable: boolean;
  pinned: boolean;
  selectable: boolean;
  selected: boolean;
  subBandIds: string[];
  url: string;
}
