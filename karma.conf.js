// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular/cli'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-firefox-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage-istanbul-reporter'),
      require('karma-junit-reporter'),
      require('@angular/cli/plugins/karma')
    ],

    files: [
      { pattern: './src/test.ts', watched: false }
      ],
    mime: {
      'text/x-typescript': ['ts','tsx']
    },    client:{
      clearContext: false // leave Jasmine Spec Runner output visible in browser
    },

    coverageIstanbulReporter: {
      reports: [ 'html', 'lcovonly' ],
      fixWebpackSourcePaths: true,
      dir: './reports/coverage', // base output directory
      'report-config': {
        html: {},
        lcovonly: { file: 'coverage.lcov' }
      }
    },
    angularCli: {
      environment: 'dev'
    },
    reporters: ['coverage-istanbul', 'progress', 'kjhtml', 'junit'],
    junitReporter : {
      outputFile: 'karma-test-results.xml'
    },
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Chrome'],
    singleRun: false
  });
};
