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
  MpsServerSourceFileState,
  MpsServerSourceGraphable,
  RavenBaseSource,
  RavenCompositeBand,
  RavenCustomFilter,
  RavenCustomFilterSource,
  RavenCustomGraphableSource,
  RavenExpandableSource,
  RavenFileMetadata,
  RavenFileSource,
  RavenFilterSource,
  RavenFolderSource,
  RavenGraphableFilterSource,
  RavenGraphableSource,
  RavenPin,
  RavenSource,
  RavenSubBand,
  StringTMap,
} from './../models';

/**
 * Transform form an MPS Server source to a Raven source.
 */
export function toSource(parentId: string, isServer: boolean, mSource: MpsServerSource): RavenSource {
  // Replace any slashes or '.' in name with dashes since slashes delineate sources in the source-explorer and '.' cannot be be used in keys in mongodb.
  const sourceName = mSource.name.replace(/\.|\//g, '-');

  const rSource: RavenBaseSource = {
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
    parentId,
    permissions: '',
    pinnable: true,
    pinned: false,
    selectable: true,
    subBandIds: [], // List of band ids that this source contributes data to.
    subKind: mSource.__kind_sub,
    type: '',
    url: '',
  };

  if (rSource.kind === 'fs_category') {
    return fromCategory(mSource as MpsServerSourceCategory, rSource);
  } else if (rSource.kind === 'fs_custom_filter') {
    return fromCustomFilter(mSource as MpsServerSourceGraphable, rSource);
  } else if (rSource.kind === 'fs_custom_graphable') {
    return fromCustomGraphable(mSource as MpsServerSourceGraphable, rSource);
  } else if (rSource.kind === 'fs_dir') {
    return fromDir(isServer, mSource as MpsServerSourceDir, rSource);
  } else if (rSource.kind === 'fs_file') {
    if (rSource.subKind === 'file_state') {
      return fromState(mSource as MpsServerSourceFileState, rSource);
    } else {
      return fromFile(mSource as MpsServerSourceFile, rSource);
    }
  } else if (rSource.kind === 'fs_filter') {
    return fromFilter(mSource as MpsServerSourceGraphable, rSource);
  } else if (rSource.kind === 'fs_graphable_filter') {
    return fromGraphableFilter(mSource as MpsServerSourceGraphable, rSource);
  } else if (rSource.kind === 'fs_graphable') {
    return fromGraphable(mSource as MpsServerSourceGraphable, rSource);
  } else {
    return rSource as RavenSource;
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
export function fromCategory(mSource: MpsServerSourceCategory, rSource: RavenBaseSource): RavenExpandableSource {
  return {
    ...rSource,
    content: mSource.contents,
    expandable: true,
    expanded: false,
    icon: 'fa fa-file-o',
    selectable: false,
    type: 'category',
  };
}

/**
 * Convert an MPS Server 'fs_dir' source to a Raven source.
 */
export function fromDir(isServer: boolean, mSource: MpsServerSourceDir, rSource: RavenBaseSource): RavenFolderSource {
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
    expandable: true,
    expanded: false,
    fileMetadata: toRavenFileMetadata(mSource as MpsServerSourceFile),
    icon: isServer ? 'fa fa-database' : 'fa fa-folder',
    permissions: mSource.permissions,
    selectable: true,
    selected: false,
    type: 'folder',
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
    expandable: true,
    expanded: false,
    fileMetadata: toRavenFileMetadata(mSource),
    icon: 'fa fa-file',
    permissions: mSource.permissions,
    selectable: true,
    selected: false,
    type: 'file',
    url: mSource.contents_url,
  };
}

/**
 * Convert an MPS Server 'fs_custom_graphable' source to a Raven source.
 */
export function fromCustomGraphable(mSource: MpsServerSourceGraphable, rSource: RavenBaseSource): RavenCustomGraphableSource {
  return {
    ...rSource,
    arg: mSource.arg,
    expandable: false,
    filterKey: mSource.filter_key,
    icon: 'fa fa-area-chart',
    openable: false,
    selectable: false,
    type: 'customGraphable',
    url: mSource.data_url,
  };
}

/**
 * Convert an MPS Server 'fs_filter' source to a Raven source.
 */
export function fromCustomFilter(mSource: MpsServerSourceGraphable, rSource: RavenBaseSource): RavenCustomFilterSource {
  return {
    ...rSource,
    expandable: false,
    filter: '',
    filterSetOf: mSource.filterSetOf,
    filterTarget: mSource.filterTarget,
    icon: 'fa fa-area-chart',
    openable: false,
    opened: false,
    selectable: true,
    type: 'customFilter',
    url: mSource.data_url,
  };
}

/**
 * Convert an MPS Server 'fs_filter' source to a Raven source.
 */
export function fromFilter(mSource: MpsServerSourceGraphable, rSource: RavenBaseSource): RavenFilterSource {
  return {
    ...rSource,
    expandable: false,
    filterSetOf: mSource.filterSetOf,
    filterTarget: mSource.filterTarget,
    icon: 'fa fa-area-chart',
    openable: false,
    opened: false,
    selectable: true,
    type: 'filter',
    url: mSource.data_url,
  };
}

/**
 * Convert an MPS Server 'fs_graphable' source to a Raven source.
 */
export function fromGraphable(mSource: MpsServerSourceGraphable, rSource: RavenBaseSource): RavenGraphableSource {
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
    opened: false,
    selectable: mSource.customMeta ? true : false,
    selected: false,
    type: 'graphable',
    url: mSource.data_url,
  };
}

/**
 * Convert an MPS Server 'fs_graphable' source to a Raven source.
 */
export function fromGraphableFilter(mSource: MpsServerSourceGraphable, rSource: RavenBaseSource): RavenGraphableFilterSource {
  return {
    ...rSource,
    expandable: false,
    filterSetOf: mSource.filterSetOf,
    filterTarget: mSource.filterTarget,
    icon: 'fa fa-area-chart',
    openable: false,
    opened: false,
    selectable: false,
    type: 'graphableFilter',
    url: mSource.data_url,
  };
}

/**
 * Convert an MPS Server 'fs_state' source to a Raven source.
 */
export function fromState(mSource: MpsServerSourceFileState, rSource: RavenBaseSource): RavenFileSource {
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
    expanded: false,
    icon: 'fa fa-table',
    openable: false,
    selectable: true,
    selected: false,
    type: 'file',
    url: mSource.file_data_url,
  };
}

/**
 * Helper for saving a state.
 * Returns list of sourceIds from subBand. sourceIds of related filters are added for graphableFilter and filters are
 * added as query options for customGraphable
 */
export function getSourceIdsForSubBand(
  sourceIds: string[],
  treeBySourceId: StringTMap<RavenSource>,
  label: string,
  customFiltersBySourceId: StringTMap<RavenCustomFilter[]> | null = null,
  filtersByTarget: StringTMap<StringTMap<RavenSource[]>> | null = null,
): string[] {
  let savedSourceIds: string[] = [];

  sourceIds.forEach(sourceId => {
    const source = treeBySourceId[sourceId];
    // Custom Graphable.
    if (source.type === 'customGraphable' && customFiltersBySourceId) {
      const customFilter = getCustomFilterForLabel(label, customFiltersBySourceId[sourceId]);
      savedSourceIds.push(customFilter ? `${source.id}?label=${customFilter.label}&filter=${customFilter.filter}` : source.id);
    } else if (source.type === 'graphableFilter' && filtersByTarget) {
      // Graphable Filter.
      savedSourceIds = getTargetFilterSourceIds(filtersByTarget, (source as RavenGraphableFilterSource).filterTarget);
    } else {
      savedSourceIds.push(sourceId);
    }
  });

  return savedSourceIds;
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
 * Helper. Returns the filter for a label for a custom source instance. Labels for custom source are required to be unique with a given custom source.
 */
export function getCustomFilterForLabel(label: string, customFilters: RavenCustomFilter[] | null = null): RavenCustomFilter | null {
  if (customFilters) {
    for (let i = 0, l = customFilters.length; i < l; ++i) {
      if (customFilters[i].label === label) {
        return customFilters[i];
      }
    }
  }
  return null;
}

/**
 * Helper that returns a nicely formatted sourceUrl for custom-graphable and graphable-filter types.
 * Just return the default sourceUrl if the type is not custom-graphable or graphable-filter.
 */
export function getFormattedSourceUrl(
  source: RavenSource,
  customFilter: RavenCustomFilter | null,
  filtersByTarget: StringTMap<StringTMap<RavenSource[]>>,
): string {
  let sourceUrl = source.url;

  // Custom Graphable.
  if (source.type === 'customGraphable' && customFilter) {
    const customGraphableSource = source as RavenCustomGraphableSource;
    const param = customGraphableSource.arg === 'filter' ? `(${customGraphableSource.filterKey}=[${customFilter.filter}])` : customFilter.filter;
    const queryOptions = `legend=${customFilter.label}&${customGraphableSource.arg}=${param}`;
    sourceUrl = `${sourceUrl}&${queryOptions}`;
  }

  // Graphable Filter.
  if (source.type === 'graphableFilter') {
    sourceUrl = getQueryOptionsForGraphableFilter(source.url, getTargetFilters(filtersByTarget, (source as RavenGraphableFilterSource).filterTarget));
  }

  return sourceUrl;
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
 * There should only be at most one pin for a sourceId
 */
export function getPinLabel(sourceId: string, pins: RavenPin[]): string {
  const pin = getPin(sourceId, pins);
  return pin ? pin.name : '';
}

/**
 * Helper that returns query options attached to a graphable filter source id or url.
 */
export function getQueryOptionsForGraphableFilter(
  sourceIdOrUrl: string,
  targetFilters: StringTMap<RavenSource[]>,
) {
  let queryOptions = '';
  for (const group of Object.keys(targetFilters)) {
    queryOptions += `${group}=${getFilters(targetFilters[group]).join(',')}&`;
  }

  return `${sourceIdOrUrl}?${queryOptions}`;
}

/**
 * Helper. Return list of filters from list of filter/graphableFilter or customFilter sources
 */
export function getFilters(sources: RavenSource[]) {
  return sources.map(source => source.type === 'customFilter' ? (source as RavenCustomFilterSource).filter : source.name);
}

/**
 * Helper. Returns filters in set for a graphable source
 */
export function getTargetFilters(filtersByTarget: StringTMap<StringTMap<RavenSource[]>> | null, filterTarget: string) {
  let targetFilters = {};

  if (filtersByTarget) {
    Object.keys(filtersByTarget).forEach(id => {
      if (id === filterTarget) {
        targetFilters = Object.assign({}, targetFilters, filtersByTarget[id]);
      }
    });
  }

  return targetFilters;
}

/**
 * Helper. Returns sourceIds for filters in set for target
 */
export function getTargetFilterSourceIds(filtersByTarget: StringTMap<StringTMap<RavenSource[]>> | null, filterTarget: string): string[] {
  const targetFilterSourceIds = {};

  if (filtersByTarget) {
    const filterGroups = filtersByTarget[filterTarget];

    Object.keys(filterGroups).forEach(group => {
      const filterSources = filterGroups[group];
      filterSources.forEach(source => {
        if (source.type === 'customFilter') {
          // add filter in sourceId for customFilter
          const customSource = source as RavenCustomFilterSource;
          targetFilterSourceIds[`${customSource.id}?${customSource.filterSetOf}=${customSource.filter}`] = `${customSource.id}?${customSource.filterSetOf}=${customSource.filter}`;
        } else {
          targetFilterSourceIds[`${source.id}`] = `${source.id}`;
        }
      });
    });
  }

  return Object.keys(targetFilterSourceIds);
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
        const hasSourceId = sourceId.match(new RegExp('([^\?]*)(.*)?'));

        if (hasSourceId) {
          sourceIds[hasSourceId[1]] = hasSourceId[1];
        } else {
          sourceIds[sourceId] = sourceId;
        }
      });
    });
  });

  return {
    parentSourceIds: Object.keys(parentSourceIds),
    sourceIds: Object.keys(sourceIds),
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
