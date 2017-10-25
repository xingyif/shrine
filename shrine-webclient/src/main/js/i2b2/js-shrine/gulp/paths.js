var appRoot = 'src/';
var outputRoot = 'dist/';
var exportSrvRoot = 'export/';
var assetRoot = 'assets/'

module.exports = {
  root: appRoot,
  source: appRoot + '**/*.js',
  html: appRoot + '**/*.html',
  sass: appRoot + '**/*.scss',
  output: outputRoot,
  exportSrv: exportSrvRoot,
  doc: './doc',
};
