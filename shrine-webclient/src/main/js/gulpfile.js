// -- globals -- //
const gulp = require('gulp');
const server = require('gulp-server-livereload');
const pmMock = require('./server/pm-mock');
const indexDir = './client/';

gulp.task('serve', function () { 
    
    pmMock.start('server');

    return gulp.src(indexDir)
        .pipe(server({
            livereload: true,
            open: true,
            port: '8000'
        }));
});
