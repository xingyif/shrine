
import jQuery from 'jquery';
class I2B2Decorator {
  window;
  
  decorate(rootContext) {
    this.__global = rootContext;
  }

  get global() {
    return this.__global;
  }

  get i2b2() {
    return this.global.i2b2;
  }

  get shrine() {
    return this.i2b2.SHRINE;
  }

  get $() {
    return jQuery;
  }

  get prototype$() {
    return this.global.$;
  }

  get prototype$$() {
    return this.global.$$;
  }

  get YAHOO() {
    return this.global.YAHOO;
  }
}
export default I2B2Decorator;
  