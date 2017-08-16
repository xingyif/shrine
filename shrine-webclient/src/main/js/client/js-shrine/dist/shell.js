System.register(['services/i2b2.pub-sub', 'services/export'], function (_export, _context) {
  "use strict";

  var I2B2PubSub, Export, _class, _temp, Shell;

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  return {
    setters: [function (_servicesI2b2PubSub) {
      I2B2PubSub = _servicesI2b2PubSub.I2B2PubSub;
    }, function (_servicesExport) {
      Export = _servicesExport.Export;
    }],
    execute: function () {
      _export('Shell', Shell = (_temp = _class = function Shell(i2b2PubSub, exp) {
        _classCallCheck(this, Shell);

        i2b2PubSub.listen();
        exp.listen();
      }, _class.inject = [I2B2PubSub, Export], _temp));

      _export('Shell', Shell);
    }
  };
});
//# sourceMappingURL=shell.js.map
