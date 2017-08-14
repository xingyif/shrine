System.register(['services/i2b2.pub-sub'], function (_export, _context) {
  "use strict";

  var I2B2PubSub, _class, _temp, Shell;

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  return {
    setters: [function (_servicesI2b2PubSub) {
      I2B2PubSub = _servicesI2b2PubSub.I2B2PubSub;
    }],
    execute: function () {
      _export('Shell', Shell = (_temp = _class = function Shell(i2b2PubSub) {
        _classCallCheck(this, Shell);

        i2b2PubSub.listen();
      }, _class.inject = [I2B2PubSub], _temp));

      _export('Shell', Shell);
    }
  };
});
//# sourceMappingURL=shell.js.map
