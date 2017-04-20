System.register([], function (_export, _context) {
  "use strict";

  var Shell;

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  return {
    setters: [],
    execute: function () {
      _export('Shell', Shell = function () {
        function Shell() {
          _classCallCheck(this, Shell);
        }

        Shell.prototype.configureRouter = function configureRouter(config, router) {

          config.title = 'SHRINE Webclient Plugin';
          config.map([{ route: 'mailto', moduleId: 'views/mailto/mailto' }, { route: ['', 'query-viewer'], moduleId: 'views/query-viewer/query-viewer' }]);

          this.router = router;
        };

        return Shell;
      }());

      _export('Shell', Shell);
    }
  };
});
//# sourceMappingURL=shell.js.map
