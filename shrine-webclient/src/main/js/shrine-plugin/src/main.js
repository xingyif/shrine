import { PLATFORM } from 'aurelia-pal';

export async function configure(aurelia) {
  aurelia.use
    .standardConfiguration()
    .developmentLogging()
    .feature(PLATFORM.moduleName('resources/index'))
    .feature(PLATFORM.moduleName('views/index'));

  await webpackIncludes();
  await aurelia.start();
  await aurelia.setRoot(PLATFORM.moduleName('shell'));
}

const webpackIncludes = () => {
  require('./views/query-status/query-status.scss');
};


