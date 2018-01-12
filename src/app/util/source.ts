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
  RavenSource,
} from './../models';

/**
 * Transform form an MPS Server source to a Raven source.
 */
export function fromSource(parentId: string, isServer: boolean, source: MpsServerSource) {
  const newSource: RavenSource = {
    actions: [],
    bandIds: {}, // Map of band ids that this source contributes data to.
    childIds: [],
    content: [],
    dbType: '',
    draggable: false,
    expandable: true,
    expanded: false,
    hasContent: false,
    icon: '',
    id: v4(),
    isServer,
    kind: source.__kind,
    label: source.label,
    menu: true,
    name: source.name,
    openable: false,
    opened: false,
    parentId,
    permissions: '',
    pinnable: true,
    pinned: false,
    selectable: true,
    selected: false,
    url: '',
  };

  if (newSource.kind === 'fs_category') {
    fromCategory(source as MpsServerSourceCategory, newSource);
  } else if (newSource.kind === 'fs_dir') {
    fromDir(isServer, source as MpsServerSourceDir, newSource);
  } else if (newSource.kind === 'fs_file') {
    fromFile(source as MpsServerSourceFile, newSource);
  } else if (newSource.kind === 'fs_graphable') {
    fromGraphable(source as MpsServerSourceGraphable, newSource);
  }

  return newSource;
}

/**
 * Transform an array of MPS Server sources to Raven sources.
 */
export function fromSources(parentId: string, isServer: boolean, sources: MpsServerSource[]): RavenSource[] {
  return sources.map(source => fromSource(parentId, isServer, source));
}

/**
 * Convert an MPS Server 'fs_category' source to a Raven source.
 * Mutates rSource.
 */
export function fromCategory(mSource: MpsServerSourceCategory, rSource: RavenSource): void {
  rSource.icon = 'fa fa-file-o';
  rSource.content = mSource.contents;

  if (rSource.content.length > 0) {
    rSource.hasContent = true;
  }
}

/**
 * Convert an MPS Server 'fs_dir' source to a Raven source.
 * Mutates rSource.
 */
export function fromDir(isServer: boolean, mSource: MpsServerSourceDir, rSource: RavenSource): void {
  if (isServer) {
    rSource.icon = 'fa fa-database';
  } else {
    rSource.icon = 'fa fa-folder';
  }

  rSource.dbType = mSource.__db_type;
  rSource.permissions = mSource.permissions;
  rSource.url = mSource.contents_url;
}

/**
 * Convert an MPS Server 'fs_file' source to a Raven source.
 * Mutates rSource.
 */
export function fromFile(mSource: MpsServerSourceFile, rSource: RavenSource): void {
  rSource.dbType = mSource.__db_type;
  rSource.icon = 'fa fa-file';
  rSource.permissions = mSource.permissions;
  rSource.url = mSource.contents_url;
}

/**
 * Convert an MPS Server 'fs_graphable' source to a Raven source.
 * Mutates rSource.
 */
export function fromGraphable(mSource: MpsServerSourceGraphable, rSource: RavenSource): void {
  rSource.expandable = false;
  rSource.icon = 'fa fa-area-chart';
  rSource.openable = true;
  rSource.selectable = false;
  rSource.url = mSource.data_url;
}
