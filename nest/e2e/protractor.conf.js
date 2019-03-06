// Protractor configuration file, see link for more information
// https://github.com/angular/protractor/blob/master/lib/config.ts

exports.config = {
  allScriptsTimeout: 11000,
  specs: ['./**/features/*.feature'],
  capabilities: {
    browserName: 'firefox',
    'moz:firefoxOptions': {
      args: [ "--headless" ]
    }
  },
  directConnect: true,
  baseUrl: 'http://localhost:4200/',
  framework: 'custom',
  frameworkPath: require.resolve('protractor-cucumber-framework'),
  cucumberOpts: {
    compiler: [],
    dryRun: false,
    format: [
      'node_modules/cucumber-pretty'
    ],
    require: ['./**/steps/*.steps.ts'],
    strict: true,
    tags: [],
  },
  onPrepare() {
    require('ts-node').register({
      project: require('path').join(__dirname, './tsconfig.e2e.json'),
    });
  },
};
