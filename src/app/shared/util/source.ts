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
  RavenFileMetadata,
  RavenPin,
  RavenSource,
  RavenSubBand,
  StringTMap,
} from './../models';

/**
 * Transform form an MPS Server source to a Raven source.
 */
export function toSource(parentId: string, isServer: boolean, mSource: MpsServerSource): RavenSource {
  // Replace any slashes in name with dashes since slashes delineate sources in the source-explorer.
  const sourceName = mSource.name.replace('/', '-');

  const rSource: RavenSource = {
    actions: [],
    childIds: [],
    content: null,
    dbType: '',
    draggable: false,
    expandable: true,
    expanded: false,
    fileMetadata: {
      createdBy: '',
      createdOn: '',
      customMetadata: null,
      fileType: '',
      lastModified: '',
      permissions: '',
    },
    icon: '',
    id: parentId === '/' ? `/${sourceName}` : `${parentId}/${sourceName}`,
    isServer,
    kind: mSource.__kind,
    label: mSource.label,
    menu: true,
    name: mSource.name,
    openable: false,
    opened: false,
    parentId,
    pinnable: true,
    pinned: false,
    selectable: true,
    selected: false,
    subBandIds: [], // List of band ids that this source contributes data to.
    subKind: mSource.__kind_sub,
    url: '',
  };

  if (rSource.kind === 'fs_category') {
    return fromCategory(mSource as MpsServerSourceCategory, rSource);
  } else if (rSource.kind === 'fs_dir') {
    return fromDir(isServer, mSource as MpsServerSourceDir, rSource);
  } else if (rSource.kind === 'fs_file') {
    if (rSource.subKind === 'file_state') {
      return fromState(mSource as any, rSource);
    } else {
      return fromFile(mSource as MpsServerSourceFile, rSource);
    }
  } else if (rSource.kind === 'fs_graphable') {
    return fromGraphable(mSource as MpsServerSourceGraphable, rSource);
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
        event: 'file-delete',
        name: 'Delete',
      },
      {
        event: 'file-metadata',
        name: 'File metadata',
      },
      {
        event: 'file-import',
        name: 'Import',
      },
      {
        event: 'pin-add',
        name: 'Add Pin',
      },
      {
        event: 'save',
        name: 'Save',
      },
    ],
    dbType: mSource.__db_type,
    fileMetadata: toRavenFileMetadata(mSource as MpsServerSourceFile),
    icon: isServer ? 'fa fa-database' : 'fa fa-folder',
    url: mSource.contents_url,
  };
}

/**
 * Convert an MPS Server 'fs_file' source to a Raven source.
 */
export function fromFile(mSource: MpsServerSourceFile, rSource: RavenSource): RavenSource {
  const actions = [
    {
      event: 'delete',
      name: 'Delete',
    },
    {
      event: 'file-metadata',
      name: 'File Metadata',
    },
  ];

  if (mSource.__kind_sub === 'file_epoch') {
    actions.push({
      event: 'epoch-load',
      name: 'Load Epoch',
    });
  } else {
    actions.push({
      event: 'pin-add',
      name: 'Add Pin',
    });
  }

  return {
    ...rSource,
    actions,
    dbType: mSource.__db_type,
    fileMetadata: toRavenFileMetadata(mSource),
    icon: 'fa fa-file',
    url: mSource.contents_url,
  };
}

/**
 * Transforms MPS Server file metadata to Raven file metadata.
 */
export function toRavenFileMetadata(mSource: MpsServerSourceFile): RavenFileMetadata {
  const fileMetadata = {
    createdBy: mSource.createdBy ? mSource.createdBy : '',
    createdOn: mSource.created ? mSource.created : '',
    customMetadata: mSource.customMeta && mSource.customMeta.length ? toRavenCustomMetadata(mSource.customMeta) : null,
    fileType: mSource.__kind === 'fs_dir' ? 'folder' : (mSource.__kind_sub === 'file_maros' ? 'Generic CSV' : mSource.__kind_sub),
    lastModified: mSource.modified ? mSource.modified : '',
    permissions: mSource.permissions ? mSource.permissions.substring(0, mSource.permissions.indexOf(' ')) : '',
  };

  return fileMetadata;
}

/**
 * Transforms MPS Server custom metadata to Raven customMetadata.
 */
export function toRavenCustomMetadata(metadata: any) {
  const customMetadata = {};

  for (let i = 0, l = metadata.length; i < l; ++i) {
    const meta = metadata[i];
    customMetadata[meta['Key']] = meta['Value'];
  }

  return customMetadata;
}

/**
 * Convert an MPS Server 'fs_graphable' source to a Raven source.
 */
export function fromGraphable(mSource: MpsServerSourceGraphable, rSource: RavenSource): RavenSource {
  const fileMetadata = toRavenFileMetadata(mSource as MpsServerSourceFile);
  return {
    ...rSource,
    actions: fileMetadata.customMetadata ? [
      {
        event: 'file-metadata',
        name: 'File metadata',
      },
    ] : [],
    expandable: false,
    fileMetadata,
    icon: 'fa fa-area-chart',
    openable: true,
    selectable: fileMetadata.customMetadata ? true : false,
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
      {
        event: 'delete',
        name: 'Delete',
      },
    ],
    expandable: false,
    icon: 'fa fa-table',
    openable: false,
    selectable: true,
    url: mSource.contents_url,
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
 * Helper that returns a pin if the given source is in the given pin array. Null otherwise.
 * If a source's parent-parent or any ancestors have a pin then we only return the pin of the nearest parent.
 */
export function getPin(sourceId: string, pins: RavenPin[]): RavenPin | null {
  const filteredPins = pins.filter(pin => sourceId.includes(pin.sourceId));

  if (filteredPins.length) {
    // The `filteredPins` list contains all pins that contain source id sub-strings of the given `sourceId`.
    // We want to reduce those pins to a single pin such that the pins source id is the longest `sourceId` sub-string.
    // Convince yourself that this is analogous to finding the closest parent pin for the given `sourceId`.
    return filteredPins.reduce((aPin, bPin) => aPin.sourceId.length >= bPin.sourceId.length ? aPin : bPin);
  }

  return null;
}

/**
 * Helper that gets a pin label from a list of pins and corresponding source ids.
 * If there is more than one pin then it returns the pins as a comma-separated string.
 */
export function getPinLabel(sourceIds: string[], pins: RavenPin[]): string {
  return sourceIds.reduce((pinNames: string[], sourceId: string) => {
    const pin = getPin(sourceId, pins);

    // If the current source id is referenced in a pin, then add that pin to the pin names.
    if (pin) {
      pinNames.push(pin.name);
    }

    return pinNames;
  }, []).join(', ');
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
