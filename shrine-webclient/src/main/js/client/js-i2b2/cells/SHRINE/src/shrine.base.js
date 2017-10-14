import $ from 'jquery';
export default class ShrineBase {
  get global() {
    return window;
  }

  get shrine() {
    return this.i2b2.SHRINE;
  }

  get i2b2() {
    return this.global.i2b2;
  }

  get $() {
    return $;
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