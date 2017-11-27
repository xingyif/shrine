'use strict';

System.register(['aurelia-framework', 'services/query-status.model', 'services/pub-sub'], function (_export, _context) {
    "use strict";

    var customElement, observable, QueryStatusModel, PubSub, _dec, _class, _desc, _value, _class2, _descriptor, _class3, _temp, QueryStatus, TIMEOUT_SECONDS, DEFAULT_VERSION, me, hubMsgTypes, initialState;

    function _initDefineProp(target, property, descriptor, context) {
        if (!descriptor) return;
        Object.defineProperty(target, property, {
            enumerable: descriptor.enumerable,
            configurable: descriptor.configurable,
            writable: descriptor.writable,
            value: descriptor.initializer ? descriptor.initializer.call(context) : void 0
        });
    }

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

    function _applyDecoratedDescriptor(target, property, decorators, descriptor, context) {
        var desc = {};
        Object['ke' + 'ys'](descriptor).forEach(function (key) {
            desc[key] = descriptor[key];
        });
        desc.enumerable = !!desc.enumerable;
        desc.configurable = !!desc.configurable;

        if ('value' in desc || desc.initializer) {
            desc.writable = true;
        }

        desc = decorators.slice().reverse().reduce(function (desc, decorator) {
            return decorator(target, property, desc) || desc;
        }, desc);

        if (context && desc.initializer !== void 0) {
            desc.value = desc.initializer ? desc.initializer.call(context) : void 0;
            desc.initializer = undefined;
        }

        if (desc.initializer === void 0) {
            Object['define' + 'Property'](target, property, desc);
            desc = null;
        }

        return desc;
    }

    function _initializerWarningHelper(descriptor, context) {
        throw new Error('Decorating class property failed. Please ensure that transform-class-properties is enabled.');
    }

    return {
        setters: [function (_aureliaFramework) {
            customElement = _aureliaFramework.customElement;
            observable = _aureliaFramework.observable;
        }, function (_servicesQueryStatusModel) {
            QueryStatusModel = _servicesQueryStatusModel.QueryStatusModel;
        }, function (_servicesPubSub) {
            PubSub = _servicesPubSub.PubSub;
        }],
        execute: function () {
            _export('QueryStatus', QueryStatus = (_dec = customElement('query-status'), _dec(_class = (_class2 = (_temp = _class3 = function (_PubSub) {
                _inherits(QueryStatus, _PubSub);

                function QueryStatus(queryStatus) {
                    _classCallCheck(this, QueryStatus);

                    for (var _len = arguments.length, rest = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
                        rest[_key - 1] = arguments[_key];
                    }

                    var _this = _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));

                    _initDefineProp(_this, 'nodes', _descriptor, _this);

                    me.set(_this, {
                        isDevEnv: document.location.href.includes('http://localhost:8000/'),
                        exportAvailable: false
                    });
                    return _this;
                }

                QueryStatus.prototype.nodesChanged = function nodesChanged(newValue, oldValue) {
                    if (!newValue || !newValue.length) {
                        me.get(this).exportAvailable = false;
                        this.publish(this.notifications.shrine.queryUnavailable);
                        return;
                    }
                    me.get(this).exportAvailable = true;
                    this.publish(this.notifications.shrine.queryAvailable);
                };

                QueryStatus.prototype.attached = function attached() {
                    var _this2 = this;

                    this.subscribe(this.notifications.i2b2.queryStarted, function (n) {
                        _this2.status = initialState().status;
                        _this2.status.updated = Number(new Date());
                        _this2.nodes = initialState().nodes;
                        _this2.hubMsg = initialState().hubMsg;
                        _this2.status.query.queryName = n;
                    });

                    this.subscribe(this.notifications.i2b2.networkIdReceived, function (d) {
                        var runningPreviousQuery = _this2.status === undefined;
                        if (runningPreviousQuery) _this2.status = initialState().status;
                        var networkId = d.networkId;

                        _this2.status.query.networkId = networkId;
                        _this2.status.updated = Number(new Date());
                        _this2.nodes = initialState().nodes;
                        _this2.hubMsg = hubMsgTypes.RESPONSE_RECEIVED;
                        _this2.publish(_this2.commands.shrine.fetchQuery, { networkId: networkId, timeoutSeconds: TIMEOUT_SECONDS, dataVersion: DEFAULT_VERSION });
                    });

                    this.subscribe(this.notifications.i2b2.exportQuery, function () {
                        var nodes = _this2.nodes;
                        _this2.publish(_this2.commands.shrine.exportResult, { nodes: nodes });
                    });

                    this.subscribe(this.notifications.i2b2.clearQuery, function () {
                        _this2.nodes = initialState().nodes;
                        _this2.status = initialState().status;
                    });
                    this.subscribe(this.notifications.shrine.queryReceived, function (data) {
                        var query = data.query,
                            nodes = data.nodes,
                            _data$dataVersion = data.dataVersion,
                            dataVersion = _data$dataVersion === undefined ? DEFAULT_VERSION : _data$dataVersion,
                            _data$query$networkId = data.query.networkId,
                            networkId = _data$query$networkId === undefined ? null : _data$query$networkId;
                        var _query$complete = query.complete,
                            complete = _query$complete === undefined ? false : _query$complete;

                        var timeoutSeconds = TIMEOUT_SECONDS;
                        if (networkId !== _this2.status.query.networkId) return;
                        var updated = Number(new Date());
                        Object.assign(_this2.status, { query: query, updated: updated });
                        _this2.nodes = nodes;
                        if (!complete) {
                            _this2.publish(_this2.commands.shrine.fetchQuery, { networkId: networkId, dataVersion: dataVersion, timeoutSeconds: timeoutSeconds });
                        } else if (_this2.nodes.length) {
                            _this2.publish(_this2.notifications.shrine.refreshAllHistory);
                        }
                    });

                    if (me.get(this).isDevEnv) {
                        this.publish(this.notifications.i2b2.queryStarted, "started query");
                        window.setTimeout(function () {
                            return _this2.publish(_this2.notifications.i2b2.networkIdReceived, { networkId: '2421519216383772161', name: "started query" });
                        }, 2000);
                    }
                };

                return QueryStatus;
            }(PubSub), _class3.inject = [QueryStatusModel], _temp), (_descriptor = _applyDecoratedDescriptor(_class2.prototype, 'nodes', [observable], {
                enumerable: true,
                initializer: null
            })), _class2)) || _class));

            _export('QueryStatus', QueryStatus);

            TIMEOUT_SECONDS = 15;
            DEFAULT_VERSION = -1;
            me = new WeakMap();
            hubMsgTypes = {
                WAITING_ON_RESPONSE: 'Waiting on response from network',
                RESPONSE_RECEIVED: 'Response received from network.  Waiting on results'
            };

            initialState = function initialState(n) {
                return { status: { query: { networkId: null, queryName: null, updated: null, complete: false } }, nodes: [], hubMsg: hubMsgTypes.WAITING_ON_RESPONSE };
            };
        }
    };
});
//# sourceMappingURL=query-status.js.map
