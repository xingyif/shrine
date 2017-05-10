System.register([], function (_export, _context) {
    "use strict";

    var I2B2Service;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _export('I2B2Service', I2B2Service = function I2B2Service() {
                var context = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : window;

                _classCallCheck(this, I2B2Service);

                var getLib = function getLib(context, lib) {
                    return hasParent(context) ? getParent(context)[lib] : null;
                };
                var getParent = function getParent(context) {
                    return context.parent.window;
                };
                var hasParent = function hasParent(context) {
                    return context && context.parent && context.parent.window;
                };
                var i2b2 = getLib(context, 'i2b2');

                this.onResize = function (f) {
                    return i2b2 ? i2b2.events.changedZoomWindows.subscribe(f) : null;
                };
                this.onHistory = function (f) {
                    return i2b2 ? i2b2.CRC.ctrlr.history.events.onDataUpdate.subscribe(f) : null;
                };
                this.loadHistory = function () {
                    return i2b2 ? i2b2.CRC.view.history.doRefreshAll() : null;
                };
                this.loadQuery = function (id) {
                    return i2b2 ? i2b2.CRC.ctrlr.QT.doQueryLoad(id) : null;
                };
            });

            _export('I2B2Service', I2B2Service);
        }
    };
});
//# sourceMappingURL=i2b2.service.js.map
