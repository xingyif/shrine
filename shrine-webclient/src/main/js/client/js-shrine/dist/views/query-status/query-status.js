System.register(['services/query-status.model', 'services/pub-sub'], function (_export, _context) {
    "use strict";

    var QueryStatusModel, PubSub, _extends, _class, _temp, QueryStatus;

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
        setters: [function (_servicesQueryStatusModel) {
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

            _export('QueryStatus', QueryStatus = (_temp = _class = function (_PubSub) {
                _inherits(QueryStatus, _PubSub);

                function QueryStatus(queryStatus) {
                    _classCallCheck(this, QueryStatus);

                    for (var _len = arguments.length, rest = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
                        rest[_key - 1] = arguments[_key];
                    }

                    var _this = _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));

                    var initialState = function initialState() {
                        return { query: { queryName: null, updated: null, complete: false }, nodes: null };
                    };
                    _this.status = initialState();

                    _this.subscribe(_this.notifications.i2b2.queryStarted, function (n) {
                        _this.status.query.queryName = n;
                    });
                    _this.subscribe(_this.notifications.i2b2.networkIdReceived, function (id) {
                        return _this.publish(_this.commands.shrine.fetchQuery, id);
                    });
                    _this.subscribe(_this.notifications.shrine.queryReceived, function (data) {
                        _this.status.query = _extends({}, _this.status.query, data.query);
                        _this.status.nodes = data.nodes;
                        _this.status.updated = Number(new Date());
                        var complete = data.query.complete;
                        var networkId = data.query.networkId;
                        if (!complete) {
                            window.setTimeout(function () {
                                return publishFetchQuery(networkId);
                            }, 10000);
                        }
                    });

                    return _this;
                }

                return QueryStatus;
            }(PubSub), _class.inject = [QueryStatusModel], _temp));

            _export('QueryStatus', QueryStatus);
        }
    };
});
//# sourceMappingURL=query-status.js.map
