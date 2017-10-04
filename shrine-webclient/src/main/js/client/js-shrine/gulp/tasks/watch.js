const gulp = require('gulp');
const paths = require('../paths');
gulp.task('watch', () => {

    gulp.watch([paths.source, paths.html, paths.sass], ['test','build']);
});
