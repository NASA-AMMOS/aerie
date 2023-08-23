function chartTimings(metric, title) {
  return {
    id: metric,
    title: title,
    series: {
      [`${metric}_trend_avg`]: { label: 'avg', width: 2, format: 'duration' },
      [`${metric}_trend_p(90)`]: { label: 'p(90)', format: 'duration' },
      [`${metric}_trend_p(95)`]: { label: 'p(95)', format: 'duration' }
    },
    axes: [{}, {format: 'duration'}, { side: 1, format: 'duration' }],
    height: 224
  }
}

const customTab = {
    id: `custom_snapshot`,
    title: `Custom Metrics`,
    event: "snapshot",
    charts: [
      chartTimings('effective_arg_duration', 'Effective Argument Request Duration'),
    ],
    report: true,
    description: 'Example tab displaying custom metrics'
};

defaultConfig.tabs.push(customTab);

export default defaultConfig;
