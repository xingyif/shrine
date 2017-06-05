import {inject} from 'aurelia-framework';
import {I2B2PubSub} from 'common/i2b2.pub-sub';
@inject(I2B2PubSub)
export class Shell {
  constructor(i2b2PubSub) {
    i2b2PubSub.listen();
  }
  configureRouter(config, router) {

    config.title = 'SHRINE Webclient Plugin';
    config.map([
      {route: 'mailto', moduleId: 'views/mailto/mailto'},
      {route: ['', 'query-viewer'], moduleId: 'views/query-viewer/query-viewer'}
    ]);

    this.router = router;
  }
}
