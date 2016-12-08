var gulp = require('gulp');

/**
 * Todo: figure out livereload issue.
 */
gulp.task('serve', function () {
    var server = require('gulp-server-livereload');
    console.log('starting server at: ' + './build/');
    return gulp.src('./build/')
        .pipe(server({
            livereload: true,
            open: true,
            port: '8000'
        }));
});
