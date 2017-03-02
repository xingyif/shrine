var gulp = require('gulp');


/**
 * todo: set up jshint
 */

/**
 * todo:  set up lint
 */

/**
 * Serve in Browser.
 */
gulp.task('serve', function () {
    const server = require('gulp-server-livereload');
    const serveDir = './';
    const port = '8000';
    const openOnLaunch = true;
    const liveReload = true;

    console.log('starting server at: ' + serveDir);
    return gulp.src(serveDir)
        .pipe(server({
            livereload: liveReload,
            open: openOnLaunch,
            port: port
        }));
});

/**
 * Run Headless Unit Tests in Karma
 */
gulp.task('test', function () {
    startTests(true, function () {
        console.log('done test log.');
    });
});

/**
 * todo: set up clean, bundle and build
 */


/**
 * Start unit tests
 */
function startTests(singleRun, done) {
    var karma = startKarma(singleRun, done);
}

function startKarma(singleRun, done) {
    var KarmaServer = require('karma').Server;
    var excludeFiles = [];

    var karma = new KarmaServer({
        configFile: __dirname + '/karma.conf.js',
        excludeFiles: excludeFiles,
        singleRun: true
    }, karmaCompleted);

    karma.start();

    function karmaCompleted(karmaResult) {
        console.log('karma completed');
        if (karmaResult === 1) {
            done('karma tests failed with code ' + karmaResult);
        } else {
            done();
        }
    }

    return karma;
}

