const gulp = require('gulp');
const $ = require('gulp-load-plugins')({ 
    lazy: true
});

gulp.task('default', function (callback) {
    //$.sequence('build')(callback);
    console.log('default');
});