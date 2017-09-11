System.register(['aurelia-event-aggregator', 'repository/qep.repository', './shrine.messages'], function (_export, _context) {
    "use strict";

    var EventAggregator, QEPRepository, commands, notifications, _extends, _class, _temp, isBusy, QueryStatusModel;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_repositoryQepRepository) {
            QEPRepository = _repositoryQepRepository.QEPRepository;
        }, function (_shrineMessages) {
            commands = _shrineMessages.commands;
            notifications = _shrineMessages.notifications;
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

            isBusy = function () {
                var inProgress = false;
                return function (v) {
                    inProgress = v === undefined ? inProgress : v ? true : false;
                    return inProgress;
                };
            }();

            _export('QueryStatusModel', QueryStatusModel = (_temp = _class = function QueryStatusModel(evtAgg, qep, notifications) {
                _classCallCheck(this, QueryStatusModel);

                var publishNetworkId = function publishNetworkId(id) {
                    return evtAgg.publish(notifications.i2b2.networkIdReceived, id);
                };
                var publishQuery = function publishQuery(model) {
                    return evtAgg.publish(notifications.shrine.queryReceived, model);
                };
                var logError = function logError(error) {
                    return console.log('ERROR: ' + error);
                };
                var toModel = function toModel(data) {
                    return new Promise(function (resolve, reject) {
                        var nodes = data.results.length === 0 ? [] : [].concat(data.results.sort(function (a, b) {
                            return a.adapterNode.toUpperCase() <= b.adapterNode.toUpperCase() ? -1 : 1;
                        }));
                        var dataVersion = data.dataVersion;
                        var complete = nodes.length > 0 && nodes.filter(function (n) {
                            return 'ERROR,COMPLETED,FINISHED'.includes(n.status);
                        }).length === nodes.length;
                        var query = _extends({}, data.query, { complete: complete });
                        resolve({
                            query: query,
                            nodes: nodes,
                            dataVersion: dataVersion
                        });
                    });
                };

                var loadNetworkId = function loadNetworkId(n) {
                    return qep.fetchNetworkId(n).then(function (result) {
                        return publishNetworkId(result);
                    }).catch(function (error) {
                        return logError(error);
                    });
                };

                var loadQuery = function loadQuery(d) {
                    qep.fetchQuery(d.networkId, d.timeoutSeconds, d.dataVersion).then(function (result) {
                        return toModel(result);
                    }).catch(function (error) {
                        return logError(error);
                    }).then(function (model) {
                        return publishQuery(model);
                    });
                };

                var init = function init() {
                    evtAgg.subscribe(commands.shrine.fetchQuery, loadQuery);
                };
                init();
            }, _class.inject = [EventAggregator, QEPRepository, notifications], _temp));

            _export('QueryStatusModel', QueryStatusModel);
        }
    };
});
//# sourceMappingURL=query-status.model.js.map
