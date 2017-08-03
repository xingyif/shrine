import {I2B2PubSub} from 'common/i2b2.pub-sub';
export class Shell {
  static inject = [I2B2PubSub];
  constructor(i2b2PubSub) {
    i2b2PubSub.listen();
  }
}
