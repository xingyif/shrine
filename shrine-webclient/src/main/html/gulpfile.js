var gulp = require('gulp');

gulp.task('default', function () {
    var server = require('gulp-server-livereload');
    console.log('starting server at: ./');
    return gulp.src('./')
        .pipe(server({
            livereload: true,
            open: true,
            port: '8000'
        }));
});