/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { v4 } from 'uuid';

import {
  MpsServerSourceCategory,
  MpsServerSourceDir,
  MpsServerSourceFile,
  MpsServerSourceGraphable,
  MpsServerSource,
  RavenSourceAction,
  StringTMap,
} from './index';

export class RavenSource {
  actions: RavenSourceAction[];
  bandIds: StringTMap<boolean>;
  childIds: string[];
  content: MpsServerSourceGraphable[];
  dbType: string;
  draggable: boolean;
  expandable: boolean;
  expanded: boolean;
  hasContent: boolean;
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
  permissions: string;
  pinnable: boolean;
  pinned: boolean;
  selectable: boolean;
  selected: boolean;
  url: string;

  /**
   * Default Constructor.
   */
  constructor(source:  MpsServerSource) {
    this.actions = [];
    this.bandIds = {}; // Map of band ids that this source contributes data to.
    this.childIds = [];
    this.content = [];
    this.dbType = '';
    this.draggable = false;
    this.expandable = true;
    this.expanded = false;
    this.hasContent = false;
    this.icon = '';
    this.id = v4();
    this.isServer = false;
    this.kind = source.__kind;
    this.label = source.label;
    this.menu = true;
    this.name = source.name;
    this.openable = false;
    this.opened = false;
    this.parentId = '';
    this.permissions = '';
    this.pinnable = true;
    this.pinned = false;
    this.selectable = true;
    this.selected = false;
    this.url = '';
  }

  /**
   * Convert an MPS Server 'fs_category' source to a Raven source.
   */
  fromCategory(parentId: string, source: MpsServerSourceCategory): void {
    this.icon = 'fa fa-file-o';
    this.parentId = parentId;
    this.content = source.contents;

    if (this.content.length > 0) {
      this.hasContent = true;
    }
  }

  /**
   * Convert an MPS Server 'fs_dir' source to a Raven source.
   */
  fromDir(parentId: string, isServer: boolean, source: MpsServerSourceDir): void {
    if (isServer) {
      this.icon = 'fa fa-database';
    } else {
      this.icon = 'fa fa-folder';
    }

    this.dbType = source.__db_type;
    this.isServer = isServer;
    this.parentId = parentId;
    this.permissions = source.permissions;
    this.url = source.contents_url;
  }

  /**
   * Convert an MPS Server 'fs_file' source to a Raven source.
   */
  fromFile(parentId: string, source: MpsServerSourceFile): void {
    this.dbType = source.__db_type;
    this.icon = 'fa fa-file';
    this.parentId = parentId;
    this.permissions = source.permissions;
    this.url = source.contents_url;
  }

  /**
   * Convert an MPS Server 'fs_graphable' source to a Raven source.
   */
  fromGraphable(parentId: string, source: MpsServerSourceGraphable): void {
    this.expandable = false;
    this.icon = 'fa fa-area-chart';
    this.openable = true;
    this.selectable = false;
    this.url = source.data_url;
  }
}
