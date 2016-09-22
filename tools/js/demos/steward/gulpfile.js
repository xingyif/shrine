
// -- globals -- //
var gulp = require('gulp');

/**
 * Todo: figure out livereload issue.
 */
gulp.task('serve', function () {
    var server = require('gulp-server-livereload');
    return gulp.src('./')
        .pipe(server({
            livereload: true,
            open: true,
            port: '8000'
        }));
});