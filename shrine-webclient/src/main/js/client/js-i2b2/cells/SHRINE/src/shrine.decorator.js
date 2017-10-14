import ShrineBase from './shrine.base';
class ShrineDecorator extends ShrineBase {
  constructor() {
    super();
  }

  decorate() {
    throw new Error(`the ShrineBase abstract method 'decorate' must be implemented`);
  }
}
export default ShrineDecorator;