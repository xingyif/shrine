import {inject} from 'aurelia-framework';
import {I2B2PubSub} from 'common/i2b2.pub-sub';

@inject(I2B2PubSub)
export class Shell {
  constructor(i2b2PubSub) {
    i2b2PubSub.listen();
  }
}
