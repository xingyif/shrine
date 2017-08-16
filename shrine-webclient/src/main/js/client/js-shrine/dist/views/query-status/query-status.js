System.register(['aurelia-framework', 'services/query-status.model', 'services/pub-sub'], function (_export, _context) {
    "use strict";

    var customElement, QueryStatusModel, PubSub, _extends, _dec, _class, _class2, _temp, QueryStatus, privateProps, initialState;

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
        setters: [function (_aureliaFramework) {
            customElement = _aureliaFramework.customElement;
        }, function (_servicesQueryStatusModel) {
            QueryStatusModel = _servicesQueryStatusModel.QueryStatusModel;
        }, function (_servicesPubSub) {
            PubSub = _servicesPubSub.PubSub;
        }],
        execute: function () {
            _extends = Object.assign || function (target) {
                for (var i = 1; i < arguments.length; i++) {
                    var source = arguments[i];

                    for (var key in source) {
                        if (Object.prototype.hasOwnProperty.call(source, key)) {
                            target[key] = source[key];
                        }
                    }
                }

                return target;
            };

            _export('QueryStatus', QueryStatus = (_dec = customElement('query-status'), _dec(_class = (_temp = _class2 = function (_PubSub) {
                _inherits(QueryStatus, _PubSub);

                function QueryStatus(queryStatus) {
                    _classCallCheck(this, QueryStatus);

                    for (var _len = arguments.length, rest = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
                        rest[_key - 1] = arguments[_key];
                    }

                    var _this = _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));

                    privateProps.set(_this, {
                        isDevEnv: document.location.href.includes('http://localhost:8000/')
                    });
                    return _this;
                }

                QueryStatus.prototype.attached = function attached() {
                    var _this2 = this;

                    this.subscribe(this.notifications.i2b2.queryStarted, function (n) {
                        _this2.status = initialState();
                        _this2.status.query.queryName = n;
                    });
                    this.subscribe(this.notifications.i2b2.networkIdReceived, function (id) {
                        return _this2.publish(_this2.commands.shrine.fetchQuery, id);
                    });
                    this.subscribe(this.notifications.shrine.queryReceived, function (data) {
                        var query = data.query;
                        var nodes = data.nodes;
                        var updated = Number(new Date());
                        var complete = data.query.complete;
                        var networkId = data.query.networkId;
                        _this2.status = _extends({}, _this2.status, { query: query, nodes: nodes, updated: updated });
                        if (!complete) {
                            window.setTimeout(function () {
                                return _this2.publish(_this2.commands.shrine.fetchQuery, networkId);
                            }, 10000);
                        } else {
                            _this2.publish(_this2.commands.shrine.exportResult, _extends({}, _this2.status));
                        }
                    });

                    if (privateProps.get(this).isDevEnv) {
                        this.publish(this.notifications.i2b2.queryStarted, "started query");
                        this.publish(this.notifications.i2b2.networkIdReceived, 1);
                    }
                };

                return QueryStatus;
            }(PubSub), _class2.inject = [QueryStatusModel], _temp)) || _class));

            _export('QueryStatus', QueryStatus);

            privateProps = new WeakMap();

            initialState = function initialState() {
                return { query: { queryName: null, updated: null, complete: false }, nodes: null };
            };
        }
    };
});
//# sourceMappingURL=query-status.js.map
