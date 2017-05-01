export class Shell {
  configureRouter(config, router) {

    config.title = 'SHRINE Webclient Plugin';
    config.map([
      {route: 'mailto', moduleId: 'views/mailto/mailto'},
      {route: ['', 'query-viewer'], moduleId: 'views/query-viewer/query-viewer'}
    ]);

    this.router = router;
  }
}
