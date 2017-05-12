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
                var applyIfExists = _.curry(function (d, f, c) {
                    return c.hasNothing() ? d : f(c);
                })(Container.of(null));
                var map = _.curry(function (el, v) {
                    return v.map(_.prop(el));
                });
                var prop = _.curry(function (el, c) {
                    return applyIfExists(map(el), c);
                });
                var i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
                var crc = _.compose(prop('CRC'), i2b2);
                var events = _.compose(prop('events'), i2b2);

                this.onResize = function (f) {
                    return applyIfExists(function (c) {
                        return c.value.changedZoomWindows.subscribe(f);
                    }, events(ctx));
                };
                this.onHistory = function (f) {
                    return applyIfExists(function (c) {
                        return c.value.ctrlr.history.events.onDataUpdate.subscribe(f);
                    }, crc(ctx));
                };
                this.onQuery = function (f) {
                    return applyIfExists(function (c) {
                        return c.value.afterQueryInit.subscribe(f);
                    }, events(ctx));
                };
                this.loadHistory = function () {
                    return applyIfExists(function (c) {
                        return c.value.view.history.doRefreshAll();
                    }, crc(ctx));
                };
                this.loadQuery = function (id) {
                    return applyIfExists(function (c) {
                        return c.value.ctrlr.QT.doQueryLoad(id);
                    }, crc(ctx));
                };
            });

            _export('I2B2Service', I2B2Service);
        }
    };
});
//# sourceMappingURL=i2b2.service.js.map
