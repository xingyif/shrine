var gulp = require('gulp');

gulp.task('serve',  ['watch'], () => {
  var server = require('gulp-server-livereload');
  const serveDir = './'

  console.log('starting server at: ' + serveDir);
  return gulp.src(serveDir)
    .pipe(server({
      livereload: true,
      open: true,
      port: '8000'
    }));
});

gulp.task('serve-src', () => {
  var server = require('gulp-server-livereload');
  const serveDir = './'

  console.log('starting server at: ' + serveDir);
  return gulp.src(serveDir)
    .pipe(server({
      livereload: true,
      open: true,
      port: '8000'
    }));
});

gulp.task('serve-export',  () => {
  var server = require('gulp-server-livereload');
  const serveDir = './export/';

  console.log('starting server at: ' + serveDir);
  return gulp.src(serveDir)
    .pipe(server({
      livereload: true,
      open: true,
      port: '8000'
    }));
});