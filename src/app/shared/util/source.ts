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
  MpsServerSource,
  MpsServerSourceCategory,
  MpsServerSourceDir,
  MpsServerSourceFile,
  MpsServerSourceGraphable,
  RavenSource,
} from './../models';

/**
 * Transform form an MPS Server source to a Raven source.
 */
export function toSource(parentId: string, isServer: boolean, source: MpsServerSource): RavenSource {
  const newSource: RavenSource = {
    actions: [],
    bandIds: {}, // Map of band ids that this source contributes data to.
    childIds: [],
    content: [],
    dbType: '',
    draggable: false,
    expandable: true,
    expanded: false,
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
    return fromCategory(source as MpsServerSourceCategory, newSource);
  } else if (newSource.kind === 'fs_dir') {
    return fromDir(isServer, source as MpsServerSourceDir, newSource);
  } else if (newSource.kind === 'fs_file') {
    return fromFile(source as MpsServerSourceFile, newSource);
  } else if (newSource.kind === 'fs_graphable') {
    return fromGraphable(source as MpsServerSourceGraphable, newSource);
  } else {
    return newSource;
  }
}

/**
 * Transform an array of MPS Server sources to Raven sources.
 */
export function toRavenSources(parentId: string, isServer: boolean, sources: MpsServerSource[]): RavenSource[] {
  return sources.map((source: MpsServerSource) => toSource(parentId, isServer, source));
}

/**
 * Convert an MPS Server 'fs_category' source to a Raven source.
 */
export function fromCategory(mSource: MpsServerSourceCategory, rSource: RavenSource): RavenSource {
  return {
    ...rSource,
    content: mSource.contents,
    icon: 'fa fa-file-o',
  };
}

/**
 * Convert an MPS Server 'fs_dir' source to a Raven source.
 */
export function fromDir(isServer: boolean, mSource: MpsServerSourceDir, rSource: RavenSource): RavenSource {
  return {
    ...rSource,
    dbType: mSource.__db_type,
    icon: isServer ? 'fa fa-database' : 'fa fa-folder',
    permissions: mSource.permissions,
    url: mSource.contents_url,
  };
}

/**
 * Convert an MPS Server 'fs_file' source to a Raven source.
 */
export function fromFile(mSource: MpsServerSourceFile, rSource: RavenSource): RavenSource {
  return {
    ...rSource,
    dbType: mSource.__db_type,
    icon: 'fa fa-file',
    permissions: mSource.permissions,
    url: mSource.contents_url,
  };
}

/**
 * Convert an MPS Server 'fs_graphable' source to a Raven source.
 */
export function fromGraphable(mSource: MpsServerSourceGraphable, rSource: RavenSource): RavenSource {
  return {
    ...rSource,
    expandable: false,
    icon: 'fa fa-area-chart',
    openable: true,
    selectable: false,
    url: mSource.data_url,
  };
}
