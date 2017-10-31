//TODO: migrating to webpack, gulp-run is a temporary workaround, want to handle all tasks in package.json!
var gulp = require('gulp');
var run = require('gulp-run');

gulp.task('test', () => {
 return run('npm t').exec() 
});
