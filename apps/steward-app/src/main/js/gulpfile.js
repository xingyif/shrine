
/**
 * Build Pipeline for Shrine Data Steward
 * 
 */

// -- globals -- //
var gulp = require('gulp');
var args = require('yargs').argv;
var config = require('./gulp.config')();
var $ = require('gulp-load-plugins')({lazy:true});

// -- code errors and style.
// -- to get both verbose and exit on error:  'gulp lint --v --e'
gulp.task('lint', function () {
    gulp.src(config.lintFiles)
    // -- detailed output with '--v' param
    .pipe($.if(args.v, $.print()))
    .pipe($.jscs())
    .pipe($.jshint())
    .pipe($.if(args.verbose, $.jshint.reporter('jshint-stylish', {verbose: true})))
    // -- exit on fail with '--e' param
    .pipe($.if(args.e, $.jshint.reporter('fail')));
});

// -- lint and run unit tests -- //
gulp.task('test', /*['lint'], */function () {
    startTests(true /* single run*/, function () {
        console.log('done test log.');
    });
});

gulp.task('clean-build', function () {
    var del = require('del');
    return del([
        config.buildDir + '**/*'
    ]);
});

gulp.task('watch-src', function () {
        gulp.watch(config.tplFiles.concat(config.index), ['build-index', 'copy-app-src']);
});

gulp.task('build', function (callback) {
    $.sequence('test', 'clean-build', ['copy-bower-styles', 'copy-bower-src', 'copy-app-assets', 'copy-app-src'], 'build-index')(callback);
});

gulp.task('default', function (callback) {
    $.sequence('build'/* todo, launch webserver and , 'webserver', 'watch-src'*/)(callback);
});

gulp.task('build-index', function () {
    var options = config.getWiredepDefaultOptions();
    var wiredep = require('wiredep').stream;

    return gulp
        .src(config.index)
        .pipe(wiredep(options))
        .pipe($.inject(gulp.src(config.jsFiles)))
        .pipe($.inject(gulp.src(config.cssFiles)))
        .pipe(gulp.dest(config.buildDir));
});

gulp.task('copy-bower-styles', function () {
    return gulp
        .src(config.bower.cssFiles)
        .pipe($.copy(config.buildDir));
});

gulp.task('copy-bower-src', function () {
    return gulp
        .src(config.bower.jsFiles)
        .pipe($.copy(config.buildDir));
});

gulp.task('copy-app-assets', function () {
    return gulp
        .src(config.assetFiles)
        .pipe($.copy(config.buildDir));
});

gulp.task('copy-app-src', function () {
    return gulp
        .src(config.jsFiles.concat(config.tplFiles))
        .pipe(gulp.dest(config.buildDir + 'app/client/'));
});

/**
 * Todo: figure out livereload issue.
 */
gulp.task('webserver', function () {
    var server = require('gulp-server-livereload');
    console.log('starting server at: ' + config.buildDir);
    return gulp.src(config.buildDir)
        .pipe(server({
            livereload: true
        }));
});

///////////

/**
 * Start unit tests
 */
function startTests (singleRun, done) {
    var karma = startKarma(singleRun, done);
}

function startKarma (singleRun, done) {
    var KarmaServer = require('karma').Server;
    var excludeFiles = [];

    var karma = new KarmaServer({
        configFile: __dirname + '/karma.conf.js',
        excludeFiles: excludeFiles,
        singleRun: true
    }, karmaCompleted);

    karma.start();

    function karmaCompleted (karmaResult) {
        console.log('karma completed');
        if (karmaResult === 1) {
            done('karma tests failed with code ' + karmaResult);
        } else {
            done();
        }
    }

    return karma;
}
