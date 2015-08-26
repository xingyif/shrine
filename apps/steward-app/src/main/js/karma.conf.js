// Karma configuration
// Generated on Wed Jan 07 2015 17:25:23 GMT-0500 (EST)

module.exports = function (config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],


    // list of files / patterns to load in the browser
    // load all files with the .spec.js extension.
        <!-- endbower -->
        <!-- endbuild -->

        <!-- build:js({.tmp,app}) scripts/scripts.js -->
    files: [
        'src/vendor/jquery/dist/jquery.min.js',
        'src/vendor/angular/angular.min.js',
        'src/vendor/bootstrap/dist/js/bootstrap.min.js',
        'src/vendor/json3/lib/json3.min.js',
        'src/vendor/angular-ui-router/release/angular-ui-router.min.js',
        'src/vendor/angular-route/angular-route.min.js',
        'src/vendor/angular-cookies/angular-cookies.min.js',
        'src/vendor/oclazyload/dist/ocLazyLoad.min.js',
        'src/vendor/lodash/lodash.min.js',
        'src/vendor/angular-ui-router/release/angular-ui-router.js',
        'src/vendor/x2js/xml2json.min.js',
        'src/vendor/angular-loading-bar/build/loading-bar.min.js',
        'src/vendor/angular-bootstrap/ui-bootstrap-tpls.js',
        'src/vendor/metisMenu/dist/metisMenu.min.js',
        'src/vendor/Chart.js/Chart.min.js',
        'src/vendor/sb-admin-2/js/sb-admin-2.js',
        'src/vendor/angular/angular-mocks.js',
        'src/app/common/utils-service.js',
        'src/app/common/model-service.js',
        'src/app/**/*.js'
    ],


    // list of files to exclude
    exclude: [
    ],




    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
    },

      plugins : [
          'karma-chrome-launcher',
          'karma-firefox-launcher',
          'karma-phantomjs-launcher',
          'karma-jasmine'
      ],

    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['PhantomJS'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true
  });
};
