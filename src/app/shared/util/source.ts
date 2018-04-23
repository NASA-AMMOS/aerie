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
    id: parentId === '/' ? `/${mSource.name}` : `${parentId}/${mSource.name}`,
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
    subBandIds: [], // List of band ids that this source contributes data to.
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
      {
        event: 'file-import',
        name: 'Import',
      },
      {
        event: 'file-delete',
        name: 'Delete',
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
    actions: mSource.__kind_sub === 'file_epoch' ? [
      {
        event: 'delete',
        name: 'Delete',
      },
      {
        event: 'epoch-load',
        name: 'Load Epoch',
      },
    ] : [{
      event: 'delete',
      name: 'Delete',
    }],
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
        event: 'apply',
        name: 'Apply',
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
  let childIds: string[] = source && source.childIds || [];

  childIds.forEach(childId => {
    const childSource = tree[childId];
    childIds = childIds.concat(getAllChildIds(tree, childSource.id));
  });

  return childIds;
}

/**
 * Helper that returns all sources by kind, starting at a given source id in the given tree.
 */
export function getAllSourcesByKind(tree: StringTMap<RavenSource>, sourceId: string, kind: string): RavenSource[] {
  let sourceNames: RavenSource[] = [];

  tree[sourceId].childIds.forEach((childId: string) => {
    const childSource = tree[childId];

    if (childSource.kind === kind) {
      sourceNames.push(childSource);
    }

    sourceNames = sourceNames.concat(getAllSourcesByKind(tree, childSource.id, kind));
  });

  return sourceNames;
}

/**
 * Helper that returns all the individual parent source ids for a given source id.
 *
 * So if a source has id: '/hello/world/goodbye'.
 * This function will break up that id into: ['/hello', '/hello/world'],
 * which are just the individual parent source ids up to (but not including) the given source id.
 */
export function getParentSourceIds(sourceId: string): string[] {
  const parentSourceIds: string[] = [];

  while (sourceId.length > 1) {
    sourceId = sourceId.replace(/\/([^\/]+)\/?$/, ''); // Removes last id name up to '/'.
    parentSourceIds.push(sourceId);
  }

  parentSourceIds.pop(); // Remove root id '/'.
  parentSourceIds.reverse(); // Return the ids starting from the top.

  return parentSourceIds;
}

/**
 * Helper that returns a list of ALL source ids for a list of bands,
 * separated by parent ids and leaf source ids.
 * Using a hash here so we don't have to filter out repeats later.
 */
export function getSourceIds(bands: RavenCompositeBand[]) {
  const parentSourceIds = {};
  const sourceIds = {};

  bands.forEach((band: RavenCompositeBand) => {
    band.subBands.forEach((subBand: RavenSubBand) => {
      subBand.sourceIds.forEach(sourceId => {
        getParentSourceIds(sourceId).forEach(id => parentSourceIds[id] = id);
        sourceIds[sourceId] = sourceId;
      });
    });
  });

  return {
    parentSourceIds: Object.keys(parentSourceIds),
    sourceIds: Object.keys(sourceIds),
  };
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

/**
 * Helper that replaces part of a source id with a base id.
 * Ex. If we have a source id: /a/b/c/d, and a base id: /x/y, then
 *     this function should output /x/y/c/d.
 */
export function updateSourceId(sourceId: string, baseId: string): string {
  const sourceIds = sourceId.split('/').filter(x => x !== '');
  const baseIds = baseId.split('/').filter(x => x !== '');

  for (let i = 0, l = sourceIds.length; i < l; ++i) {
    if (baseIds[i] !== undefined) {
      sourceIds.splice(i, 1, baseIds[i]);
    }
  }

  sourceIds.unshift('');

  return sourceIds.join('/');
}
