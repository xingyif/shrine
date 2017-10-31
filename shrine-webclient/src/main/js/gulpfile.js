// -- globals -- //
const gulp = require('gulp');
const server = require('gulp-server-livereload');
const pmMock = require('./server/pm-mock');
const indexDir = './i2b2/';

gulp.task('serve', ['pm'], function () { 

    return gulp.src(indexDir)
        .pipe(server({
            livereload: true,
            open: true,
            port: '8000'
        }));
});

gulp.task('pm', function () {
    pmMock.start('server');
});
