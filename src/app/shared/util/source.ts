/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  MpsServerSource,
  MpsServerSourceCategory,
  MpsServerSourceDir,
  MpsServerSourceFile,
  MpsServerSourceGraphable,
  RavenCompositeBand,
  RavenSource,
  RavenSubBand,
  StringTMap,
} from './../models';

/**
 * Transform form an MPS Server source to a Raven source.
 */
export function toSource(parentId: string, isServer: boolean, mSource: MpsServerSource): RavenSource {
  const rSource: RavenSource = {
    actions: [],
    childIds: [],
    content: [],
    dbType: '',
    draggable: false,
    expandable: true,
    expanded: false,
    icon: '',
    id: `${parentId}${mSource.name}/`,
    isServer,
    kind: mSource.__kind,
    label: mSource.label,
    menu: true,
    name: mSource.name,
    openable: false,
    opened: false,
    parentId,
    permissions: '',
    pinnable: true,
    pinned: false,
    selectable: true,
    selected: false,
    subBandIds: {}, // Map of band ids that this source contributes data to.
    url: '',
  };

  if (rSource.kind === 'fs_category') {
    return fromCategory(mSource as MpsServerSourceCategory, rSource);
  } else if (rSource.kind === 'fs_dir') {
    return fromDir(isServer, mSource as MpsServerSourceDir, rSource);
  } else if (rSource.kind === 'fs_file') {
    return fromFile(mSource as MpsServerSourceFile, rSource);
  } else if (rSource.kind === 'fs_graphable') {
    if (rSource.name.includes('raven2-state')) {
      // TODO: Replace 'any' with a concrete type.
      return fromState(mSource as any, rSource);
    } else {
      return fromGraphable(mSource as MpsServerSourceGraphable, rSource);
    }
  } else {
    return rSource;
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
    actions: [
      {
        event: 'save',
        name: 'Save',
      },
    ],
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
    actions: [
      {
        event: 'delete',
        name: 'Delete',
      },
    ],
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

/**
 * Convert an MPS Server 'fs_state' source to a Raven source.
 */
export function fromState(mSource: any, rSource: RavenSource): RavenSource {
  return {
    ...rSource,
    actions: [
      {
        event: 'load',
        name: 'Load',
      },
    ],
    expandable: false,
    icon: 'fa fa-table',
    openable: false,
    selectable: true,
    url: mSource.data_url,
  };
}

/**
 * Helper that returns a list of all child ids (recursively) for a given source id.
 */
export function getAllChildIds(tree: StringTMap<RavenSource>, sourceId: string): string[] {
  const source = tree[sourceId];
  let childIds = source.childIds;

  childIds.forEach(childId => {
    const childSource = tree[childId];
    childIds = childIds.concat(getAllChildIds(tree, childSource.id));
  });

  return childIds;
}

/**
 * Helper that returns the parent node for a given sourceId. Null otherwise.
 */
export function getParent(tree: StringTMap<RavenSource>, sourceId: string): RavenSource | null {
  const treeIds = Object.keys(tree);

  for (let i = 0, l = treeIds.length; i < l; ++i) {
    const treeId = treeIds[i];
    const source = tree[treeId];

    if (source.childIds.includes(sourceId)) {
      return source;
    }
  }

  return null;
}

/**
 * Helper that returns all the individual source id parent paths for a given source id.
 *
 * So if a source has path: '/hello/world/goodbye/'.
 * This function will break up that path into: ['/hello/', '/hello/world/'],
 * which are just the individual source id parent paths up to (but not including) the given source.
 */
export function getParentPaths(sourceId: string): string[] {
  const sources = sourceId.split('/').filter(p => p !== '');
  sources.pop();

  const paths: string[] = [];

  for (let i = 0, l = sources.length; i < l; ++i) {
    if (paths[i - 1]) {
      paths.push(`${paths[i - 1]}${sources[i]}/`);
    } else {
      paths.push(`/${sources[i]}/`);
    }
  }

  return paths;
}

/**
 * Helper that returns a list of ALL parent source ids for a list of bands.
 */
export function getParentSourceIds(bands: RavenCompositeBand[]): string[] {
  const sourceIds = {};

  bands.forEach((band: RavenCompositeBand) => {
    band.subBands.forEach((subBand: RavenSubBand) => {
      Object.keys(subBand.sourceIds).forEach(sourceId => {
        getParentPaths(sourceId).forEach(id => sourceIds[id] = id);
      });
    });
  });

  return Object.keys(sourceIds);
}

/**
 * Helper that returns a source type based on a given source id. Empty otherwise.
 */
export function getSourceType(sourceId: string): string {
  if (sourceId.includes('Activities by Legend')) {
    return 'byLegend';
  } else if (sourceId.includes('Activities by Type')) {
    return 'byType';
  } else {
    console.warn('source.ts - getSourceType: unknown source type: ', sourceId);
    return '';
  }
}
