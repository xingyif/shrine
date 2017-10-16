import ShrineBase from './shrine.base';
class I2B2Decorator extends ShrineBase {
  constructor() {
    super();
  }

  decorate() {
    throw new Error(`the I2B2Decorator abstract method 'decorate' must be implemented`);
  }
}
export default I2B2Decorator;
  