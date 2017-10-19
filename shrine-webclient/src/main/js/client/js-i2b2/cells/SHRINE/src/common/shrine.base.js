import jQuery from 'jquery';
export default class ShrineBase {

  constructor() {
    // create i2b2 event lifecycle.
    this.i2b2.events.afterCellInit.subscribe(function(en,co) {
      console.log('after cell init!');
    });
  }

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