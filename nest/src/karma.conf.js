// Karma configuration file, see link for more information.
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    files: [
      '../libs/ctl/ctl.libs.min.js',
      '../libs/ctl/ctl.js',
      '../node_modules/@angular/material/prebuilt-themes/indigo-pink.css',
      '../node_modules/font-awesome/css/font-awesome.min.css',
      '../node_modules/ag-grid-community/dist/styles/ag-grid.css',
      '../node_modules/ag-grid-community/dist/styles/ag-theme-fresh.css',
      '../node_modules/ngx-toastr/toastr.css',
      './styles.css',
      '../libs/ctl/ctl.css',
    ],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-firefox-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage-istanbul-reporter'),
      require('karma-junit-reporter'),
      require('@angular-devkit/build-angular/plugins/karma'),
    ],
    client: {
      clearContext: false, // Leave Jasmine Spec Runner output visible in browser.
    },
    coverageIstanbulReporter: {
      dir: require('path').join(__dirname, '../coverage'),
      reports: ['html', 'lcovonly', 'text', 'text-summary'],
      fixWebpackSourcePaths: true,
    },
    junitReporter: {
      // Put the junit results in the top-level directory so it's not watched by Karma.
      outputFile: '../../karma-test-results.xml',
    },
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox'],
      },
    },
    reporters: ['progress', 'kjhtml', 'junit'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    // If developers want to see the window, this can be changed back to Chrome
    // and the --browsers ChromeHeadlessNoSandbox value can be added to the
    // npm test command
    browsers: ['ChromeHeadlessNoSandbox'],
    singleRun: false,
  });
};
