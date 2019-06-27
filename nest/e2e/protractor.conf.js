const { SpecReporter } = require('jasmine-spec-reporter');

exports.config = {
  allScriptsTimeout: 11000,
  specs: [
    './src/planning/plans.e2e-spec.ts',
    './src/sequencing/sequencing.pageinit.e2e-spec.ts',
    './src/sequencing/sequencing.codemirror.e2e-spec.ts',
    './src/sequencing/sequencing.commanddictionary.e2e-spec.ts',
    './src/sequencing/sequencing.panels.e2e-spec.ts',
    './src/sequencing/sequencing.toolbar.e2e-spec.ts',
    './src/sequencing/sequencing.parameter-editor.e2e-spec.ts',
  ],
  capabilities: {
    browserName: 'chrome',
    shardTestFiles: false,
    maxInstances: 1,
  },
  directConnect: true,
  baseUrl: 'http://localhost:4200',
  framework: 'jasmine',
  jasmineNodeOpts: {
    showColors: true,
    defaultTimeoutInterval: 30000,
    print: function() {},
  },
  resultJsonOutputFile: './e2e/results/result.json',
  onPrepare() {
    require('ts-node').register({
      project: require('path').join(__dirname, './tsconfig.e2e.json'),
    });
    jasmine
      .getEnv()
      .addReporter(new SpecReporter({ spec: { displayStacktrace: true } }));
  },
};
