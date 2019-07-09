const { SpecReporter } = require('jasmine-spec-reporter');

exports.config = {
  allScriptsTimeout: 11000,
  specs: [
    './src/falcon/pageinit.e2e-spec.ts',
    './src/falcon/codemirror.e2e-spec.ts',
    './src/falcon/command-dictionary.e2e-spec.ts',
    './src/falcon/panels.e2e-spec.ts',
    './src/falcon/toolbar.e2e-spec.ts',
    './src/falcon/parameter-editor.e2e-spec.ts',
    './src/merlin/plans.e2e-spec.ts',
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
