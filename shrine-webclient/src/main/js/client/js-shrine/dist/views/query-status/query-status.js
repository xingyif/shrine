System.register(['aurelia-event-aggregator', 'common/shrine.messages', 'common/query-status.model'], function (_export, _context) {
    "use strict";

    var EventAggregator, notifications, commands, QueryStatusModel, _extends, _class, _temp, QueryStatus;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_commonShrineMessages) {
            notifications = _commonShrineMessages.notifications;
            commands = _commonShrineMessages.commands;
        }, function (_commonQueryStatusModel) {
            QueryStatusModel = _commonQueryStatusModel.QueryStatusModel;
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

            _export('QueryStatus', QueryStatus = (_temp = _class = function QueryStatus(evtAgg, notifications, commands, queryStatus) {
                var _this = this;

                _classCallCheck(this, QueryStatus);

                var initialState = function initialState() {
                    return { query: { queryName: null, updated: null, complete: false }, nodes: null };
                };
                this.status = initialState();

                var publishFetchQuery = function publishFetchQuery(id) {
                    return evtAgg.publish(commands.shrine.fetchQuery, id);
                };

                evtAgg.subscribe(notifications.i2b2.queryStarted, function (n) {
                    _this.status.query.queryName = n;
                });
                evtAgg.subscribe(notifications.i2b2.networkIdReceived, function (id) {
                    return publishFetchQuery(id);
                });
                evtAgg.subscribe(notifications.shrine.queryReceived, function (data) {
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

                evtAgg.publish(notifications.i2b2.queryStarted, "started query");
                evtAgg.publish(notifications.i2b2.networkIdReceived, 1);
            }, _class.inject = [EventAggregator, notifications, commands, QueryStatusModel], _temp));

            _export('QueryStatus', QueryStatus);
        }
    };
});
//# sourceMappingURL=query-status.js.map
