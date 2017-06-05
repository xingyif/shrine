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
      _export('Shell', Shell = (_dec = inject(I2B2PubSub), _dec(_class = function () {
        function Shell(i2b2PubSub) {
          _classCallCheck(this, Shell);

          i2b2PubSub.listen();
        }

        Shell.prototype.configureRouter = function configureRouter(config, router) {

          config.title = 'SHRINE Webclient Plugin';
          config.map([{ route: 'mailto', moduleId: 'views/mailto/mailto' }, { route: ['', 'query-viewer'], moduleId: 'views/query-viewer/query-viewer' }]);

          this.router = router;
        };

        return Shell;
      }()) || _class));

      _export('Shell', Shell);
    }
  };
});
//# sourceMappingURL=shell.js.map
