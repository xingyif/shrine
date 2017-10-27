'use strict';

System.register(['./pub-sub', './i2b2.service'], function (_export, _context) {
    "use strict";

    var PubSub, I2B2Service, _class, _temp, I2B2PubSub;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    function _possibleConstructorReturn(self, call) {
        if (!self) {
            throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
        }

        return call && (typeof call === "object" || typeof call === "function") ? call : self;
    }

    function _inherits(subClass, superClass) {
        if (typeof superClass !== "function" && superClass !== null) {
            throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
        }

        subClass.prototype = Object.create(superClass && superClass.prototype, {
            constructor: {
                value: subClass,
                enumerable: false,
                writable: true,
                configurable: true
            }
        });
        if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
    }

    return {
        setters: [function (_pubSub) {
            PubSub = _pubSub.PubSub;
        }, function (_i2b2Service) {
            I2B2Service = _i2b2Service.I2B2Service;
        }],
        execute: function () {
            _export('I2B2PubSub', I2B2PubSub = (_temp = _class = function (_PubSub) {
                _inherits(I2B2PubSub, _PubSub);

                function I2B2PubSub(i2b2Svc) {
                    _classCallCheck(this, I2B2PubSub);

                    for (var _len = arguments.length, rest = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
                        rest[_key - 1] = arguments[_key];
                    }

                    var _this = _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));

                    _this.listen = function () {
                        i2b2Svc.onResize(function (a, b) {
                            return b.find(function (e) {
                                return e.action === 'ADD';
                            }) ? function () {
                                return _this.publish(_this.notifications.i2b2.tabMax);
                            } : function () {
                                return _this.publish(_this.notifications.i2b2.tabMin);
                            };
                        });
                        i2b2Svc.onHistory(function () {
                            return _this.publish(_this.notifications.i2b2.historyRefreshed);
                        });
                        i2b2Svc.onQuery(function (e, d) {
                            return _this.publish(_this.notifications.i2b2.queryStarted, d[0].name);
                        });
                        i2b2Svc.onNetworkId(function (e, d) {
                            return _this.publish(_this.notifications.i2b2.networkIdReceived, d[0]);
                        });
                        i2b2Svc.onViewSelected(function (e) {
                            return _this.publish(_this.notifications.i2b2.viewSelected, e.data);
                        });
                        i2b2Svc.onExport(function () {
                            return _this.publish(_this.notifications.i2b2.exportQuery);
                        });
                        i2b2Svc.onClearQuery(function () {
                            return _this.publish(_this.notifications.i2b2.clearQuery);
                        });
                        _this.subscribe(_this.commands.i2b2.cloneQuery, function (d) {
                            return i2b2Svc.loadQuery(d);
                        });
                        _this.subscribe(_this.commands.i2b2.showError, function (d) {
                            i2b2Svc.errorDetail(d);
                        });
                        _this.subscribe(_this.commands.i2b2.renameQuery, function (d) {
                            return i2b2Svc.renameQuery(d);
                        });
                        _this.subscribe(_this.commands.i2b2.flagQuery, function (d) {
                            return i2b2Svc.flagQuery(d);
                        });
                        _this.subscribe(_this.commands.i2b2.unflagQuery, function (d) {
                            return i2b2Svc.unflagQuery(d);
                        });
                        _this.subscribe(_this.notifications.shrine.queryUnavailable, function () {
                            return i2b2Svc.publishQueryUnavailable();
                        });
                        _this.subscribe(_this.notifications.shrine.queryAvailable, function () {
                            return i2b2Svc.publishQueryAvailable();
                        });
                        _this.subscribe(_this.notifications.shrine.refreshAllHistory, function () {
                            return i2b2Svc.loadHistory();
                        });
                    };
                    return _this;
                }

                return I2B2PubSub;
            }(PubSub), _class.inject = [I2B2Service], _temp));

            _export('I2B2PubSub', I2B2PubSub);
        }
    };
});
//# sourceMappingURL=i2b2.pub-sub.js.map
