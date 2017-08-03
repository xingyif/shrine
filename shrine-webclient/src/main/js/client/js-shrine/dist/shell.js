System.register(['aurelia-framework', 'common/i2b2.pub-sub'], function (_export, _context) {
  "use strict";

  var inject, I2B2PubSub, _dec, _class, Shell;

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  return {
    setters: [function (_aureliaFramework) {
      inject = _aureliaFramework.inject;
    }, function (_commonI2b2PubSub) {
      I2B2PubSub = _commonI2b2PubSub.I2B2PubSub;
    }],
    execute: function () {
      _export('Shell', Shell = (_dec = inject(I2B2PubSub), _dec(_class = function Shell(i2b2PubSub) {
        _classCallCheck(this, Shell);

        i2b2PubSub.listen();
      }) || _class));

      _export('Shell', Shell);
    }
  };
});
//# sourceMappingURL=shell.js.map
