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

                var ctx = context ? Container.of(context) : Container.of(null);
                var prop = _.curry(function (m, c) {
                    return c.value ? Container.of(_.prop(m, c.value)) : Container.of(null);
                });
                var i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
                var crc = _.compose(prop('CRC'), i2b2);
                var events = _.compose(prop('events'), i2b2);
                var shrine = _.compose(prop('SHRINE'), i2b2);

                I2B2Service.prototype.onResize = function (f) {
                    return events(ctx).map(function (v) {
                        return v.changedZoomWindows.subscribe(f);
                    });
                };
                I2B2Service.prototype.onHistory = function (f) {
                    return crc(ctx).map(function (v) {
                        return v.ctrlr.history.events.onDataUpdate.subscribe(f);
                    });
                };
                I2B2Service.prototype.onQuery = function (f) {
                    return events(ctx).map(function (v) {
                        return v.afterQueryInit.subscribe(f);
                    });
                };
                I2B2Service.prototype.onViewSelected = function (f) {
                    return prop('addEventListener', ctx).value ? Container.of(ctx.value.addEventListener('message', f, false)) : Container.of(null);
                };

                I2B2Service.prototype.loadHistory = function () {
                    return crc(ctx).map(function (v) {
                        return v.view.history.doRefreshAll();
                    });
                };
                I2B2Service.prototype.loadQuery = function (id) {
                    return crc(ctx).map(function (v) {
                        return v.ctrlr.QT.doQueryLoad(id);
                    });
                };
                I2B2Service.prototype.errorDetail = function (d) {
                    return shrine(ctx).map(function (v) {
                        return v.plugin.errorDetail(d);
                    });
                };
            });

            _export('I2B2Service', I2B2Service);
        }
    };
});
//# sourceMappingURL=i2b2.service.js.map
