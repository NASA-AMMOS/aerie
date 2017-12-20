/* global _, window */

window.bands = [];

window.activityMetadata = window.setArrayTrackingMode['Timeline Metadata'];
window.activityPoints = window.FalconUtils.activityToPoints(window.setArrayTrackingMode['Timeline Data']);

window.activityCompositeMetadata = window.telecom['Timeline Metadata'];
window.activityCompositePoints = window.FalconUtils.activityToPoints(window.telecom['Timeline Data']);

window.resourceCompositeMetadata = window.downlinkDataRate['Timeline Metadata'];
window.resourceCompositePoints = window.FalconUtils.resourceToPoints(window.downlinkDataRate['Timeline Data']);

window.resourceMetadata = window.spacecraftSunRange['Timeline Metadata'];
window.resourcePoints = window.FalconUtils.resourceToPoints(window.spacecraftSunRange['Timeline Data']);

window.stateMetadata = window.eclipse['Timeline Metadata'];
window.statePoints = window.FalconUtils.stringXdrToPoints(window.eclipse['Timeline Data']);

(function (b) {
  const activityBand = {
    activityStyle: 1,
    alignLabel: 3,
    baselineLabel: 3,
    height: 50,
    heightPadding: 10,
    id: _.uniqueId(),
    label: window.activityMetadata.hasObjectName,
    labelColor: [0, 0, 0],
    layout: 1,
    maxTimeRange: window.FalconUtils.getMaxTimeRange(window.activityPoints),
    minorLabels: [],
    name: window.activityMetadata.hasObjectName,
    points: window.activityPoints,
    showLabel: true,
    showTooltip: true,
    trimLabel: true,
    type: 'activity',
  };

  const compositeBandId = _.uniqueId();

  const compositeBand = {
    bands: [
      {
        activityStyle: 1,
        alignLabel: 3,
        baselineLabel: 3,
        height: 100,
        heightPadding: 10,
        id: _.uniqueId(),
        label: window.activityCompositeMetadata.hasObjectName,
        labelColor: [0, 0, 0],
        layout: 1,
        maxTimeRange: window.FalconUtils.getMaxTimeRange(window.activityCompositePoints),
        minorLabels: [''],
        name: window.activityCompositeMetadata.hasObjectName,
        parentId: compositeBandId,
        points: window.activityCompositePoints,
        showLabel: true,
        showTooltip: true,
        trimLabel: true,
        type: 'activity',
      },
      {
        autoTickValues: true,
        color: [100, 0, 255],
        fill: false,
        fillColor: [0, 0, 0],
        height: 100,
        heightPadding: 10,
        id: _.uniqueId(),
        interpolation: window.resourceCompositeMetadata.hasInterpolatorType,
        label: window.resourceCompositeMetadata.hasObjectName,
        labelColor: [0, 0, 0],
        maxTimeRange: window.FalconUtils.getMaxTimeRange(window.resourceCompositePoints),
        minorLabels: [''],
        name: window.resourceCompositeMetadata.hasObjectName,
        parentId: compositeBandId,
        points: window.resourceCompositePoints,
        rescale: true,
        showIcon: true,
        showTooltip: true,
        type: 'resource',
      },
    ],
    height: 100,
    heightPadding: 10,
    id: compositeBandId,
    name: 'Telecom & Downlink Data Rate',
    showTooltip: true,
    south: true,
    type: 'composite',
  };

  const dividerBand = {
    color: [255, 255, 255],
    height: 7,
    id: _.uniqueId(),
    label: 'Generic Divider',
    labelColor: [0, 0, 0],
    minorLabels: [],
    name: 'Generic Divider',
    showTooltip: true,
    type: 'divider',
  };

  const resourceBand = {
    autoTickValues: true,
    color: [0, 0, 0],
    fill: false,
    fillColor: [0, 0, 0],
    height: 100,
    heightPadding: 10,
    id: _.uniqueId(),
    interpolation: 'none',
    label: window.resourceMetadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange: window.FalconUtils.getMaxTimeRange(window.resourcePoints),
    minorLabels: ['AU'],
    name: window.resourceMetadata.hasObjectName,
    points: window.resourcePoints,
    rescale: true,
    showIcon: true,
    showTooltip: true,
    type: 'resource',
  };

  const stateBand = {
    alignLabel: 3,
    baselineLabel: 3,
    height: 30,
    heightPadding: 0,
    id: _.uniqueId(),
    label: window.stateMetadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange: window.FalconUtils.getMaxTimeRange(window.statePoints),
    minorLabels: [],
    name: window.stateMetadata.hasObjectName,
    points: window.statePoints,
    showTooltip: true,
    type: 'state',
  };

  b.push(activityBand);
  b.push(compositeBand);
  b.push(dividerBand);
  b.push(resourceBand);
  b.push(stateBand);
}(window.bands));
