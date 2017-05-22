System.register(['ramda', './container'], function (_export, _context) {
    "use strict";

    var _, Container, I2B2Service;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_ramda) {
            _ = _ramda;
        }, function (_container) {
            Container = _container.Container;
        }],
        execute: function () {
            _export('I2B2Service', I2B2Service = function I2B2Service() {
                var context = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : window;

                _classCallCheck(this, I2B2Service);

                var ctx = Container.of(context);
                var prop = _.curry(function (el, c) {
                    return c.value ? Container.of(_.prop(el, c.value)) : Container.of(null);
                });
                var i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
                var crc = _.compose(prop('CRC'), i2b2);
                var events = _.compose(prop('events'), i2b2);

                this.onResize = function (f) {
                    return events(ctx).map(function (v) {
                        return v.changedZoomWindows.subscribe(f);
                    });
                };
                this.onHistory = function (f) {
                    return crc(ctx).map(function (v) {
                        return v.ctrlr.history.events.onDataUpdate.subscribe(f);
                    });
                };
                this.onQuery = function (f) {
                    return events(ctx).map(function (v) {
                        return v.afterQueryInit.subscribe(f);
                    });
                };
                this.loadHistory = function () {
                    return crc(ctx).map(function (v) {
                        return v.view.history.doRefreshAll();
                    });
                };
                this.loadQuery = function (id) {
                    return crc(ctx).map(function (v) {
                        return v.ctrlr.QT.doQueryLoad(id);
                    });
                };
            });

            _export('I2B2Service', I2B2Service);
        }
    };
});
//# sourceMappingURL=i2b2.service.js.map
