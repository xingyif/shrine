/ -- globals -- //
const gulp = require('gulp');
const buildDir = './html';

gulp.task('serve', function () {
    var server = require('gulp-server-livereload');
    console.log('starting server at: ' + buildDir);
    return gulp.src(buildDir)
        .pipe(server({
            livereload: true,
            open: true,
            port: '8000'
        }));
});