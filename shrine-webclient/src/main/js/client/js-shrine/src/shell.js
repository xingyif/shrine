import {I2B2PubSub} from 'services/i2b2.pub-sub';
import {Export} from 'services/export';
export class Shell {
  static inject = [I2B2PubSub, Export];
  constructor(i2b2PubSub, exp) {
    i2b2PubSub.listen();
    exp.listen();
  }
}







