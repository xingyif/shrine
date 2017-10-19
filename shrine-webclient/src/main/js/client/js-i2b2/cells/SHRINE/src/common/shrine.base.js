import jQuery from 'jquery';
export default class ShrineBase {

  constructor() {
    // create i2b2 event lifecycle.
    this.i2b2.events.afterCellInit.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.afterAllCellsLoaded.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.afterFrameworkInit.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.afterHiveInit.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.afterLogin.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.changedViewMode.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.changedZoomWindows.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events.initView.subscribe(function(en,co) {
      console.log('after cell init!');
    });

    this.i2b2.events._privRemoveInitFuncs.subscribe(function(en,co) {
      console.log('after cell init!');
    });
    this.i2b2.events._privLoadCells.subscribe(function(en,co) {
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